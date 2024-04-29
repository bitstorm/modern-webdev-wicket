# modern-webdev-wicket

ccc

```
SERVER_PORT=8080 mvn spring-boot:run
```

init

```
public void init()
{
	super.init();

	
	// add your configuration here
	WicketWebjars.install(this);
}
```

renderHead

```
@Override
public void renderHead(IHeaderResponse response) {
	super.renderHead(response);

	response.render(CssHeaderItem.forReference(
               new WebjarsCssResourceReference("bootstrap/current/css/bootstrap.min.css")));

}
```

