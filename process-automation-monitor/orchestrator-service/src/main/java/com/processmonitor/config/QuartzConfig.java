package com.processmonitor.config;

import com.processmonitor.repository.JobRepository;
import com.processmonitor.scheduler.QuartzJobRunner;
import com.processmonitor.service.WorkerClientService;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(ApplicationContext applicationContext) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(new AutowiringSpringBeanJobFactory(applicationContext));
        factory.setOverwriteExistingJobs(true);
        factory.setStartupDelay(5);
        return factory;
    }

    static class AutowiringSpringBeanJobFactory extends AdaptableJobFactory
            implements ApplicationContextAware {

        private AutowireCapableBeanFactory beanFactory;

        AutowiringSpringBeanJobFactory(ApplicationContext applicationContext) {
            this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            Object job = super.createJobInstance(bundle);

            if (job instanceof QuartzJobRunner runner) {
                runner.setJobRepository(beanFactory.getBean(JobRepository.class));
                runner.setWorkerClientService(beanFactory.getBean(WorkerClientService.class));
            }

            beanFactory.autowireBean(job);
            return job;
        }
    }
}
