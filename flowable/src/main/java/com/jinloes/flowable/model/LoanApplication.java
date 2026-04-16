package com.jinloes.flowable.model;

/** Loan application request submitted by an applicant. */
public record LoanApplication(String applicantName, int loanAmount, int annualIncome) {}
