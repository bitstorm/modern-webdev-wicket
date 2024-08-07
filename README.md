# Modern Web development with Apache Wicket, Spring Boot, Hazelcast and WebJars


When it comes to implement web applications, Java developers usually feel lost with modern web technologies and they might think that nowadays it's not possible to implement robust and maintainable web applications without adopting the standard JavaScript-based development stack. But what if I tell you that Java is a first-class platform also for web development and that you don't need to switch to a different technology?

The following is a list of _howto_ and example projects that show you how to use Apache Wicket and other familiar frameworks and tools from the Java ecosystem (like Hazelcast, Spring Boot, WebJars, Apache Maven, etc...) to build modern and scalable applications without leaving the Java platform.

More in details you will see how to reach the following goals:

- [Producing clean and resurce-friendly URLs](#produce-resource-friendly-urls)
- [Managing CSS and JavaScript resources with WebJars and Maven](#manage-css-and-javascript-libraries-with-webjars-and-maven)
- [Scaling your application with session clustering and caching](#use-spring-boot-and-hazelcast-to-scale-your-application-with-session-clustering-and-caching)
- [Styling your application using SCSS](#style-your-application-with-scss)

> [!NOTE]
> The following examples are based on Wicket 10 and Java 21, although they should work also for Wicket 9 and Java 17

## Produce resource-friendly URLs

#### Page mounting

Wicket already comes with a native solution to generate structured and resource-friendly URLs by mounting pages to a specific path:

```java
mountPage("/path/to/page", MountedPage.class);
```

The path used for mounted pages can contain also segments with dynamic values and they are declared using a special syntax:

```java
/*
 In the following example the path used to mount UserPage has a required parameter
 (userId) and an optional one (taxId).

 For example the following path are both valid:
  - "/user/123/details/ABC1234567"
  - "/user/123/details"
*/

mountPage("/user/${userId}/details/#{taxId}", UserPage.class);
```

For a full description of page mounting see the related [user guide paragraph](https://nightlies.apache.org/wicket/guide/10.x/single.html#_generating_structured_and_clear_urls)


#### Remove page id from URL

By default Wicket uses a _versioning_ system for stateful pages assiging a incremental id to each version of the pages. This id is usually appended as query parameter at the end of the page's URL:

```
www.myhost.net/page-path?1234
```

The purpose of page versioning is to support browser’s back button: when this button is pressed Wicket must respond by rendering the same page instance previously used.
Again, for a full description of this mechanism see the related [user guide paragraph](https://nightlies.apache.org/wicket/guide/10.x/single.html#_page_versioning_and_caching)

Usually having this id at the end of the page URL is not a big deal, but sometimes you might prefer simply hiding it in the final URL.  

```java
public class NoPageIdMapper extends MountedMapper {

    public NoPageIdMapper(String mountPath, Class<? extends IRequestablePage> pageClass) {
        super(mountPath, pageClass);
    }

    @Override
    protected void encodePageComponentInfo(Url url, PageComponentInfo info) {
        //if componentInfo is null we have a page url and we skip page parameters, otherwise we keep them
        if (info.getComponentInfo() != null) {
            super.encodePageComponentInfo(url, info);
        }

    }
}
```
Please note that this mapper will remove version id only for page URLs, so stateful behaviors (like AJAX behaviors) will continue to work as usual.  

Once we created our custom mapper we must use it to mount our pages: 

```java
public void init()
{
	super.init();

	NoPageIdMapper mapper = new NoPageIdMapper(path, pageClass);
	mount(mapper);
}
```

> [!WARNING]
> Keep in mind that by removing the page id from URL you will lost the browser’s back button support.

## Manage CSS and JavaScript libraries with WebJars and Maven

WebJars is a project aimed to provide client-side libraries distributions as Maven dependency. In this way these libraries can be read directly from JAR files as regular dependecies. WebJars comes with numerous Java libraries to easily integrate this framework with the most popular web frameworks, Wicket included.

For example (project _wicket-webjars_) let's say we want to use Bootstrap 5.3.3 in our Wicket application. The first step is to include the following dependecies in our pom.xml:

```xml
<dependency>
    <groupId>de.agilecoders.wicket.webjars</groupId>
    <artifactId>wicket-webjars</artifactId>
    <version>4.0.3</version>
</dependency>

<dependency>
    <groupId>org.webjars.npm</groupId>
    <artifactId>bootstrap</artifactId>
    <version>5.3.3</version>
</dependency>
```

The first dependency is the library that allows to use WebJars with Wicket while the second is the Bootstrap library distributed by WebJars project.
The second configuration step is the initialization of _wicket-webjars_ library with the following simple code line in our application _init()_ method:

```java
public void init()
{
	super.init();

	// init wicket-webjars library
	WicketWebjars.install(this);
}
```

Now we can add Bootstrap to our page as Wicket CssHeaderItem using reference class _WebjarsCssResourceReference_

```java
@Override
public void renderHead(IHeaderResponse response) {
	super.renderHead(response);

	response.render(CssHeaderItem.forReference(
               new WebjarsCssResourceReference("bootstrap/5.3.3/css/bootstrap.min.css")));

}
```

The path used with _WebjarsCssResourceReference_ is appendend to _META-INF/resources/webjars/_ to obtain the path to the desired file inside the library jar. See the [official WebJars site](https://www.webjars.org) to have a look at the content of jar libraries.

To automatically use the version of a WebJar library from your pom.xml, we can simply replace the version in path with the _current_ string. When a resource name is resolved this string will be replaced with the most recent available version in classpath: 


```java
@Override
public void renderHead(IHeaderResponse response) {
	super.renderHead(response);

	response.render(CssHeaderItem.forReference(
               new WebjarsCssResourceReference("bootstrap/current/css/bootstrap.min.css")));

}
```

It is also possible to use a resource directly from html markup prepending _/webjars/_ to the resource path:

```html
<link rel='stylesheet' href='/webjars/bootstrap/5.3.3/css/bootstrap.min.css'>
```

> [!WARNING]
> If you are using Jetty remember that resource can be used from html only from version 12.

The project can be started with command `mvn jetty:run`. The page can be seen opening your browser at [http://localhost:8080](http://localhost:8080)

## Use Spring Boot and Hazelcast to scale your application with session clustering and caching

Scaling a web application is not a trivial task and it usually involves a lot of work on additional architectural aspects such as caching, services orchestration and replication, etc... Java developers can count on different valuable frameworks that can dramatically help handling those aspects providing a distributed data storage that can be used both as caching service and coordinator between two or more JVM. One of these framework is [Hazelcast](https://hazelcast.com/) which can be used also for web session clustering. 

In this example (project _wicket-hazelcast_) we will see how to use integrate Wicket with Spring Boot and Hazelcast to share and replicate web session among two or more server instances making our application fault tolerant and scalable.

Our application is a Spring Boot-based web application using Apache Wicket. Let's see the required dependecies to our pom.xml:


```xml
<!-- SESSION REPLICATION -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-core</artifactId>
    <version>3.2.2</version>
</dependency>

<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-hazelcast</artifactId>
    <version>3.2.2</version>          
</dependency>

<!-- WICKET SPRING BOOT INTEGRATION -->
<dependency>
  <groupId>com.giffing.wicket.spring.boot.starter</groupId>
  <artifactId>wicket-spring-boot-starter</artifactId>
  <version>4.0.0</version>        
</dependency>

<!-- WICKET HAZELCAST INTEGRATION -->
<dependency>
    <groupId>org.wicketstuff</groupId>
    <artifactId>wicketstuff-datastore-hazelcast</artifactId>
    <version>10.0.0</version>
</dependency>

<!-- SPRING HAZELCAST INTEGRATION (for caching) -->
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast-spring</artifactId>
    <version>5.3.6</version>
</dependency>
```

The main dependency is probably the one on [Wicket and Spring Boot integration project](https://github.com/MarcGiffing/wicket-spring-boot) (artifactId _wicket-spring-boot-starter_) which lays the foundation for our application.
The other dependencies are for Hazelcast integration with Spring and Wicket and for web session clustering.

Now let's look at the code starting with the configuration required to create an _HazelcastConfig_ instance for our application. This is basically the code used in the official [Hazelcast tutorial](https://docs.hazelcast.com/tutorials/spring-session-hazelcast)


```java
@Configuration
@EnableHazelcastHttpSession
@EnableCaching
public class HazelcastConfig {

    @SpringSessionHazelcastInstance
    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();

        JoinConfig join = config.getNetworkConfig().getJoin();
        // enabling multicast for autodiscovery.
        join.getMulticastConfig().setEnabled(true);

        AttributeConfig attributeConfig = new AttributeConfig()
                .setName(HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE)
                .setExtractorClassName(PrincipalNameExtractor.class.getName());

        config.getMapConfig(HazelcastIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME)
            .addAttributeConfig(attributeConfig).addIndexConfig(
                new IndexConfig(IndexType.HASH, HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE));
        
        // use custom serializer for better performances. This is optional.
        SerializerConfig serializerConfig = new SerializerConfig();
        serializerConfig.setImplementation(new HazelcastSessionSerializer()).setTypeClass(MapSession.class);
        config.getSerializationConfig().addSerializerConfig(serializerConfig);

        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }

}
```

In the class above we used two annotation (beside _@Configuration_), one to enable session clustering with Hazelcast (_@EnableHazelcastHttpSession_) and another to enable Spring caching support (_@EnableCaching_) backed by Hazelcast. Spring caching requires to create a bean of type _CacheManager_

> [!NOTE]
> Spring caching is enabled only for illustration purpose as it's not used in the example code. However with a _CacheManager_ bean created, you can use Spring annotations to [cache the results of you services](https://www.baeldung.com/spring-cache-tutorial).

> [!WARNING]
> Please note that for sake of simplicity we enabled multicast for autodiscovery, so Hazelcast will automatically add to the cluster any new application instance visible on our local network. Keep in mind that multicast is usually not suited for production environment where a safer join configuration is usually required. See the [Hazelcast documentation](https://docs.hazelcast.com/hazelcast/5.4/clusters/network-configuration) for more information on network configuration.


As final configuration step we must tell Wicket to store statefull page instances using Hazelcast. This is done inside Application _init()_ method registering a custom _PageManagerProvider_ using class _HazelcastDataStore_ from WicketStuff project. We also use class _SessionQuotaManagingDataStore_ to limit page storing to max 4 instances per session:

```java
@Override
public void init()
{
	super.init();

	// add your configuration here
	HazelcastInstance instance = getApplicationContext().getBean(HazelcastInstance.class);

	setPageManagerProvider(new DefaultPageManagerProvider(this) {
	    @Override
	    protected IPageStore newPersistentStore() {
		HazelcastDataStore hazelcastDataStore = new HazelcastDataStore(getName(), instance);
	
		return new SessionQuotaManagingDataStore(hazelcastDataStore, 4);
	    }
	});
}
```

With all configuration code in place we can start our application with the following command (assuming port 8083 is free on our machine).

```
SERVER_PORT=8083 mvn spring-boot:run
```

Taking a look at our application logs we can see a message from Hazelcast confirming that a new cluster has been created and the application has successfully joined it:

```
2024-06-13 11:39:30.169 [main] INFO  com.hazelcast.core.LifecycleService - [10.3.0.8]:5702 [dev] [5.3.6] [10.3.0.8]:5702 is STARTING
2024-06-13 11:39:32.835 [main] INFO  c.h.internal.cluster.ClusterService - [10.3.0.8]:5702 [dev] [5.3.6] 

Members {size:1, ver:1} [
	Member [10.3.0.8]:5702 - 9cf568db-8106-40d0-8463-6ca2d2082eb6 this
]
```

Once the application is up we can open our browser at [http://localhost:8083](http://localhost:8083) and check the given sessionId value. Now let's start a second instance of our application. We expect it tojoin the existing cluster and using the same shared web session. The application can be started with the same command seen above but using a different available port:

```
SERVER_PORT=8084 mvn spring-boot:run
```

Again, looking at the logs of both this new instance or the existing one we should see that the new one has joined the cluster:

```
2024-06-13 11:51:35.757 [hz.gallant_kapitsa.IO.thread-in-0] INFO  c.h.i.server.tcp.TcpServerConnection - [10.3.0.8]:5703 [dev] [5.3.6] Initialized new cluster connection between /10.3.0.8:43349 and /10.3.0.8:5702
2024-06-13 11:51:41.000 [hz.gallant_kapitsa.priority-generic-operation.thread-0] INFO  c.h.internal.cluster.ClusterService - [10.3.0.8]:5703 [dev] [5.3.6] 

Members {size:2, ver:2} [
	Member [10.3.0.8]:5702 - 9cf568db-8106-40d0-8463-6ca2d2082eb6
	Member [10.3.0.8]:5703 - bf396942-563d-4750-a0ba-0bac3e241fc8 this
]
```

Opening our browser at [http://localhost:8084](http://localhost:8084) we should have the confirm that the new instance is using the same session with the same id.
Feel free to play around stopping/restarting one of the two instances at a time to see that the session isn't lost as long as one instance is still active. 

## Style your application with SCSS

When it comes to web application styling, SCSS is a precious ally as it allows to use a more advanced syntax to manage and organize our css resources. Since SCSS needs to be converted in standard CSS language, we need a compiler to perform this task.

For developers it would be even better if this compiler could operate "live", automatically compiling SCSS sources as they are modified. Most of the time this time of compiler requires to use a dedicated external application or some kind of IDE extention to monitor our SCSS files and recompile them as they get modified. \
With Wicket we can use library _wicket-bootstrap-sass_ that offers an even more flexible solution in the form of [CSS resource](https://nightlies.apache.org/wicket/guide/10.x/single.html#_resource_management_with_wicket) that points to a SCSS file and compiles it on the fly, without depending on an external application.

> [!NOTE]
> Library _wicket-bootstrap-sass_ depends on OS library [libsass](https://github.com/sass/libsass), so be sure to have it already installed before running the following example code.

Example project _wicket-scss_ uses both library _wicket-bootstrap-sass_ and _WebJars_ to show how to easily customize Bootstrap 5 style using a SCSS file that extends the default _bootstrap.scss_ file distributed with WebJars dependency.

The project has the same dependencies seen for project _wicket-webjar_ in addition to module _wicket-bootstrap-sass_:

```xml
<dependency>
    <groupId>de.agilecoders.wicket.webjars</groupId>
    <artifactId>wicket-webjars</artifactId>
    <version>4.0.3</version>
</dependency>

<dependency>
    <groupId>de.agilecoders.wicket</groupId>
    <artifactId>wicket-bootstrap-sass</artifactId>
    <version>7.0.3</version>
</dependency>

<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>bootstrap</artifactId>
    <version>5.3.3</version>
</dependency>
```

In our application's _init()_ method we initialize both WebJars and SASS integration: 

```java
@Override
public void init()
{
    super.init();

   
    // init wicket WebJars and SASS library
    WicketWebjars.install(this);
    BootstrapSass.install(this);
}
```

Next, let's have a look at the file _custom-css.scss_ we will use to customize our Boostrap 5 based theme:

```scss
//SCSS VARIABLE OVERRIDING
$primary: #397EB4;
$warning: #f19027;
$min-contrast-ratio: 3;


//INCLUDING MAIN BOOTSTRAP SCSSS
@import "webjars!bootstrap/current/scss/bootstrap.scss";
```

The file has a starting section where we override some of the Bootstrap variables (see [official documentation](https://getbootstrap.com/docs/5.0/customize/sass/#modify-map)) to customize colors for primary and warning buttons. \
The last line imports the main Bootstrap 5.3.3 SCSS which is loaded from the corresponding WebJar using the syntax _webjars!<path_to_file>_

Finally, our file _custom-css.scss_ can be used as regular Wicket CSS header item using class _SassResourceReference_ that takes care of compilation behind the scenes:

```java
protected final CssReferenceHeaderItem customCss = 
    CssHeaderItem.forReference(new SassResourceReference(HomePage.class, "custom-css.scss"));

@Override
public void renderHead(IHeaderResponse response) {
    response.render(customCss);
}
```

Once the application is started (with the usual command `mvn jetty:run`.) you can play around modifying file _custom-css.scss_ and see changes in real time.
