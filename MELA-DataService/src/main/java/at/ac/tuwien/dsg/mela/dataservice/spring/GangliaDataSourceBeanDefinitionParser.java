package at.ac.tuwien.dsg.mela.dataservice.spring;

import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.GangliaDataSource;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Created by omoser on 1/17/14.
 */
public class GangliaDataSourceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    public Class getBeanClass(Element element) {
        return GangliaDataSource.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        String host = element.getAttribute("host");
        String port = element.getAttribute("port");
        String pollingIntervalMs = element.getAttribute("polling-interval-ms");

        builder.addPropertyValue("hostname", host);
        builder.addPropertyValue("port", Integer.valueOf(port));
        if (StringUtils.hasText(pollingIntervalMs)) {
            builder.addPropertyValue("pollingIntervalMs", Integer.valueOf(pollingIntervalMs));
        }

    }
}
