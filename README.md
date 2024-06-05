# Modern Web development with Apache Wicket


When it comes to implement web applications, Java developers usually feel lost with modern web technologies and they might think that nowadays it's not possible to implement robust and maintainable web applications without adopting the standard JavaScript-based development stack. But what if I tell you that Java is a powerful platform also for web development and that you don't need to switch to a different technology?

The following is a list of _howto_ and example projects that show you how to use Apache Wicket and other familiar frameworks and tools from the Java ecosystem (like Hazelcast, Spring Boot, WebJars, Apache Maven, etc...) to build modern and scalable applications without leaving the Java platform.

More in details you will see how to reach the following goals:

- Producing clean and resurce-friendly URLs
- Managing CSS and JavaScript resources with WebJars and Maven (project wicket-webjars)
- Scaling your application with session clustering and caching
- Styling your application using SCSS

### Produce resurce-friendly URLs

#### Page mounting

Wicket already comes with a native solution to generate structured and resurce-friendly URLs by mounting pages to a specific path:

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

### Manage CSS and JavaScript libraries with WebJars and Maven

WebJars is a project aimed to provide client-side libraries distributions as Maven dependency. In this way these libraries can be read directly from JAR files as regular dependecies. WebJars comes with numerous Java libraries to easily integrate this framework with the most popular web frameworks, Wicket included.

For example (project wicket-webjars) let's say we want to use Bootstrap 5.3.3 in our Wicket application. The first step is to include the following dependecies in our pom.xml:

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
The second configuration step is the initialization of wicket-webjars library with the following simple code line in our application _init()_ method:

```java
public void init()
{
	super.init();

	// init wicket-webjars library
	WicketWebjars.install(this);
}
```

Now we can add Bootstrap to our page as Wicket CssHeaderItem using refernce class _WebjarsCssResourceReference_

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
> If you are using Jetty embedded resource can be used from html only from version 12.

### Use Spring Boot and Hazelcast to scale your application with session clustering and caching

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

The main dependency is probably the one on [Wicket and Spring Boot integration project](https://github.com/MarcGiffing/wicket-spring-boot) (artifactId _wicket-spring-boot-starter_) which lay the foundation for our application.
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
> Please note that for sake of semplicity we enabled multicast for autodiscovery, so Hazelcast will automatically add to the cluster any new application instance visible on our local network. Keep in mind that multicast is usually not suited for production enviorment where a safier join configuration is usually required. See the [Hazelcast documentation](https://docs.hazelcast.com/hazelcast/5.4/clusters/network-configuration) for more information on network configuration.


As final configuration step we must tell Wicket to store statefull page instances using Hazelcast. This is done inside Application _init()_ method registering a custom _PAgeManagerProvider_ using class _HazelcastDataStore_ from WicketStuff project. We also use class _SessionQuotaManagingDataStore_ to limit page storing to max 5 instances per session:

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

Hazelcast start

```
SERVER_PORT=8080 mvn spring-boot:run
```

### Style your application eith SCSS

scss pom

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

scss init

```java
@Override
public void init()
{
    super.init();

   
    // add your configuration here
    WicketWebjars.install(this);
    BootstrapSass.install(this);
}
```

scss resource

```java
protected final CssReferenceHeaderItem customCss = 
    CssHeaderItem.forReference(new SassResourceReference(HomePage.class, "custom-css.scss"));

@Override
public void renderHead(IHeaderResponse response) {
    response.render(customCss);
}
```    
