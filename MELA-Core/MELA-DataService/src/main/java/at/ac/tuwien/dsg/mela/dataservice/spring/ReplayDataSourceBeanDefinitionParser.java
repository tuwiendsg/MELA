package at.ac.tuwien.dsg.mela.dataservice.spring;

import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.ReplayFromSQLDataAccess;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Created by omoser on 1/17/14.
 */
public class ReplayDataSourceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    public Class getBeanClass(Element element) {
        return ReplayFromSQLDataAccess.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        String serviceID = element.getAttribute("monitoringSequenceID");
        builder.addPropertyValue("monitoringSequenceID", serviceID);

        String pollingIntervalMs = element.getAttribute("polling-interval-ms");

        if (StringUtils.hasText(pollingIntervalMs)) {
            builder.addPropertyValue("pollingIntervalMs", Integer.valueOf(pollingIntervalMs));
        }
    }
}
