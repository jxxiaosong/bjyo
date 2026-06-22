package com.bjyo.quartz.util;

import org.quartz.JobExecutionContext;
import com.bjyo.quartz.domain.SysJob;

/**
 * 定时任务处理（允许并发执行）
 * 
 * @author bjyo
 *
 */
public class QuartzJobExecution extends AbstractQuartzJob
{
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
