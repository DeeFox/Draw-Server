package main.java.utils;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Created by DFX on 28.06.2016.
 */
@Plugin(name="PushOverAppender", category="Core", elementType="appender", printObject=true)
public class PushOverAppenderImpl extends AbstractAppender {

    private final String user;
    private final String token;

    public PushOverAppenderImpl(String name, Filter filter, Layout<? extends Serializable> layout, String tk, String usr) {
        super(name, filter, layout);
        this.token = tk;
        this.user = usr;
    }

    @Override
    public void append(LogEvent event) {
        if(!event.getLevel().equals(Level.ERROR) && !event.getLevel().equals(Level.FATAL)) {
            return;
        }
        final byte[] res = getLayout().toByteArray(event);

        sendViaPushOver(event, res);
    }

    private void sendViaPushOver(LogEvent event, byte[] res) {
        final List<NameValuePair> params = Form.form()
                .add("token", this.token)
                .add("user", this.user)
                .add("message", event.getMessage().toString())
                .add("title", new String(res))
                .build();
        try {
            Request.Post("https://api.pushover.net/1/messages.json").bodyForm(params, Consts.UTF_8).execute().returnResponse();
        } catch (IOException e) {
            // Ignore
        }
    }

    @PluginFactory
    public static PushOverAppenderImpl createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("token") String token,
            @PluginAttribute("userkey") String user) {
        if (name == null || token == null || user == null) {
            LOGGER.error("No name, token or userkey provided for PushOverAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
            PatternLayout ptl = (PatternLayout) layout;
            System.out.println(ptl.getConversionPattern());
        }
        return new PushOverAppenderImpl(name, filter, layout, token, user);
    }
}
