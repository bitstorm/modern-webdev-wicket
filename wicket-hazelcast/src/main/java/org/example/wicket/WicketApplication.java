package org.example.wicket;

import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.csp.CSPDirective;
import org.apache.wicket.csp.CSPDirectiveSrcValue;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.pageStore.IPageStore;
import org.springframework.stereotype.Component;
import org.wicketstuff.datastores.common.SessionQuotaManagingDataStore;
import org.wicketstuff.datastores.hazelcast.HazelcastDataStore;

import com.giffing.wicket.spring.boot.starter.app.WicketBootStandardWebApplication;
import com.hazelcast.core.HazelcastInstance;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 *
 * @see org.example.wicket.Start#main(String[])
 */
@Component
public class WicketApplication extends WicketBootStandardWebApplication
{
	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends WebPage> getHomePage()
	{
		return HomePage.class;
	}

	/**
	 * @see org.apache.wicket.Application#init()
	 */
	@Override
	public void init()
	{
		super.init();

		// needed for the styling used by the quickstart
		getCspSettings().blocking()
			.add(CSPDirective.STYLE_SRC, CSPDirectiveSrcValue.SELF)
			.add(CSPDirective.STYLE_SRC, "https://fonts.googleapis.com/css")
			.add(CSPDirective.FONT_SRC, "https://fonts.gstatic.com");

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
}
