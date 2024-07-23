package com.camunda.training;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.community.process_test_coverage.junit5.platform7.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
@Deployment(resources = {"Exercise5.bpmn"})
public class PaymentUnitTest {

    @Test
    public void happy_path_test_shouldChargeFromCreditCard() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("PaymentProcessEx5",
                withVariables("orderTotal", 45.99, "customerId", "cust30"));

        assertThat(processInstance).isWaitingAt(findId("Deduct customer credit")).externalTask().hasTopicName("creditDeduction");

        complete(externalTask(), withVariables("openAmount", 15.99, "customerCredit", 30.0));
        assertThat(processInstance).variables().contains(entry("openAmount", 15.99), entry("customerCredit", 30.0));

        assertThat(processInstance).isWaitingAt(findId("Charge credit card")).externalTask().hasTopicName("creditCardCharging");

        complete(externalTask(), withVariables("cardNumber", "1234 5678", "CVC", "123", "expiryDate", "09/24", "openAmount", 45.99));
        assertThat(processInstance).isWaitingAt(findId("Payment completed")).externalTask()
                .hasTopicName("paymentCompletion");

        complete(externalTask());
    }

    @Test
    public void happy_path_test_shouldDeductFromCredit() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("PaymentProcessEx5",
                withVariables("orderTotal", 45.99, "customerId", "cust60"));

        assertThat(processInstance).isWaitingAt(findId("Deduct customer credit")).externalTask().hasTopicName("creditDeduction");
        complete(externalTask(), withVariables("openAmount", 0.0, "customerCredit", 60));
        assertThat(processInstance).variables().contains(entry("openAmount", 0.0), entry("customerCredit", 60));

        assertThat(processInstance).isWaitingAt(findId("Payment completed")).externalTask()
                .hasTopicName("paymentCompletion");

        complete(externalTask());
    }

    @Test
    public void credit_card_failure_test() {
        ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcessEx5")
                .startBeforeActivity(findId("Charge credit card")).execute();

        assertThat(processInstance).isWaitingAt(findId("Charge credit card"));

        fetchAndLock("creditCardCharging", "junit-test-worker", 1);
        externalTaskService().handleBpmnError(externalTask().getId(), "junit-test-worker", "creditCardChargeError");

        assertThat(processInstance).isWaitingAt(findId("Payment failed")).externalTask().hasTopicName("paymentCompletion");

    }

    @Test
    public void customer_credit_compensation_test() {
        ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcessEx5")
                .startBeforeActivity(findId("Deduct customer credit")).setVariables(withVariables("orderTotal", 50)).execute();
        assertThat(processInstance).isWaitingAt(findId("Deduct customer credit"));

        complete(externalTask(), withVariables("customerCredit", 30));

        fetchAndLock("creditCardCharging", "junit-test-worker", 1);
        externalTaskService().handleBpmnError(externalTask().getId(), "junit-test-worker", "creditCardChargeError");

        assertThat(processInstance).isWaitingAt(findId("Customer credit restored"), findId("Restore customer credit"))
                .externalTask().hasTopicName("creditRestore");
    }

}
