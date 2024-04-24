package org.example.wicket;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import de.agilecoders.wicket.sass.SassResourceReference;

public class HomePage extends WebPage {
	private static final long serialVersionUID = 1L;

	protected static final CssReferenceHeaderItem customCss = CssHeaderItem.forReference(new SassResourceReference(HomePage.class, "custom-css.scss"));

	public HomePage(final PageParameters parameters) {
		super(parameters);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
	    response.render(customCss);
	}
}
