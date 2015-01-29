package at.ac.tuwien.dsg.mela.dataservice.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Created by omoser on 1/17/14.
 */
public class MelaNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("ganglia-datasource", new GangliaDataSourceBeanDefinitionParser());
        registerBeanDefinitionParser("replay-datasource", new ReplayDataSourceBeanDefinitionParser());
        registerBeanDefinitionParser("mela-push-datasource", new PushDataSourceBeanDefinitionParser());
        registerBeanDefinitionParser("ganglia-push-datasource", new GangliaPushDataSourceBeanDefinitionParser());
    }
}
