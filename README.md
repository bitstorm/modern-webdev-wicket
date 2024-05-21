# Modern Web development with Apache Wicket


When it comes to implement web applications, Java developers usually feel lost with modern web technologies and they might think that nowadays it's not possible to implement robust and maintainable web applications without adopting the standard JavaScript-based development stack. But what if I tell you that Java is a powerful platform also for web development and that you don't need to switch to a different technology?

The following is a list of _howto_ and examples that show you how to use Apache Wicket and other familiar frameworks and tools from Java ecosystem (like Hazelcast, Spring Boot, WebJars, Apache Maven, etc...) to build modern and scalable applications without leaving the Java platform.

More in details you will see how to reach the following goals:

- Producing clean and resurce-friendly URLs
- Managing CSS and JavaScript resources with WebJars and Maven
- Scaling your application with session clustering and caching
- Styling your application using SCSS

### Produce resurce-friendly URLs

https://nightlies.apache.org/wicket/guide/10.x/single.html#_generating_structured_and_clear_urls

```java
mountPage("/pageMount/${requiredParam}/otherSegm/#{optionalParam}", MountedPage.class);
```



no id mapper

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
no id mapper use


```java
public void init()
{
	super.init();

	NoPageIdMapper mapper = new NoPageIdMapper(path, pageClass);
	mount(mapper);
}
```

### Manage CSS and JavaScript libraries with WebJars and Maven

WebJars is a project aimed to provide client-side libraries distributions as Maven dependency. Using classpath URLs we can read SCSS files directly from our WebJars dependency. For example let's say we are using the WebJars dependency for Bootstrap 5.3.3:

```xml
<dependency>
    <groupId>org.webjars.npm</groupId>
    <artifactId>bootstrap</artifactId>
    <version>5.3.3</version>
</dependency>
```

init

```java
public void init()
{
	super.init();

	// add your configuration here
	WicketWebjars.install(this);
}
```

renderHead

```java
@Override
public void renderHead(IHeaderResponse response) {
	super.renderHead(response);

	response.render(CssHeaderItem.forReference(
               new WebjarsCssResourceReference("bootstrap/current/css/bootstrap.min.css")));

}
```


### Use Spring Boot and Hazelcast to scale your application with session clustering and caching

xml hazelcast

```xml
<!-- SESSION REPLICATION -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-core</artifactId>
    <version>3.2.2</version>
</dependency>

<!-- SPRING BOOT CONFIG -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-hazelcast</artifactId>
    <version>3.2.2</version>          
</dependency>

<dependency>
  <groupId>com.giffing.wicket.spring.boot.starter</groupId>
  <artifactId>wicket-spring-boot-starter</artifactId>
  <version>4.0.0</version>        
</dependency>

<!-- HAZELCAST CONFIG -->
<dependency>
    <groupId>org.wicketstuff</groupId>
    <artifactId>wicketstuff-datastore-hazelcast</artifactId>
    <version>10.0.0</version>
</dependency>

<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast-spring</artifactId>
    <version>5.3.6</version>
</dependency>
```

java hazelcast

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
