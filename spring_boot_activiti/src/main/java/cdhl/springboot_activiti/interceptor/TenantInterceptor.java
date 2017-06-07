package cdhl.springboot_activiti.interceptor;

import cdhl.springboot_activiti.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Created by pc on 2016-12-28.
 */

@Component
public class TenantInterceptor extends EmptyInterceptor {


    Session session;

    public void setSession(Session session) {
        this.session=session;
    }
    /*
     * entity - POJO对象
     * id - POJO对象的主键
     * state - POJO对象的每一个属性所组成的集合(除了ID)
     * propertyNames - POJO对象的每一个属性名字组成的集合(除了ID)
     * types - POJO对象的每一个属性类型所对应的Hibernate类型组成的集合(除了ID)
     */
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        String tenantCode = SecurityUtils.getCurrentUserTenant();

        if(StringUtils.isNotBlank(tenantCode) && !"anonymousUser".equals(tenantCode)) {
            for (int index = 0; index < propertyNames.length; index++) {
                /*找到名为"tenancyCode"的属性*/
                if ("tenancyCode".equals(propertyNames[index])) {
                    /*使用拦截器将对象的"tenancyCode"属性赋上值*/
                    state[index] = tenantCode;
                    return true;
                }
            }
        }

        return false;
    }

}
