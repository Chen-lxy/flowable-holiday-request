package org.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

//模拟经理通过之后调用一个远程服务类
public class CallExternalSystemDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("Calling the external system for employee"
                    + delegateExecution.getVariable("employee"));
    }
}
