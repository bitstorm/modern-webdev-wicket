# modern-webdev-wicket

ccc

```
SERVER_PORT=8080 mvn spring-boot:run
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
        join.getMulticastConfig().setEnabled(true);

        AttributeConfig attributeConfig = new AttributeConfig()
                .setName(HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE)
                .setExtractorClassName(PrincipalNameExtractor.class.getName());

        config.getMapConfig(HazelcastIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME)
            .addAttributeConfig(attributeConfig).addIndexConfig(
                new IndexConfig(IndexType.HASH, HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE));

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