package com.grameenphone.wipro.task_executor.test;

public aspect TestNGAnnotationAspects {
    pointcut beforeSuiteAnnotation(): execution(@org.testng.annotations.BeforeSuite * *.*(..));
    pointcut afterSuiteAnnotation(): execution(@org.testng.annotations.AfterSuite * *.*(..));
    pointcut beforeTestAnnotation(): execution(@org.testng.annotations.BeforeTest * *.*(..));
    pointcut afterTestAnnotation(): execution(@org.testng.annotations.AfterTest * *.*(..));
    pointcut beforeGroupsAnnotation(): execution(@org.testng.annotations.BeforeGroups * *.*(..));
    pointcut afterGroupsAnnotation(): execution(@org.testng.annotations.AfterGroups * *.*(..));
    pointcut beforeClassAnnotation(): execution(@org.testng.annotations.BeforeClass * *.*(..));
    pointcut afterClassAnnotation(): execution(@org.testng.annotations.AfterClass * *.*(..));
    pointcut beforeMethodAnnotation(): execution(@org.testng.annotations.BeforeMethod * *.*(..));
    pointcut afterMethodAnnotation(): execution(@org.testng.annotations.AfterMethod * *.*(..));

    void around(): beforeSuiteAnnotation() || afterSuiteAnnotation() || beforeTestAnnotation() || afterTestAnnotation() || beforeGroupsAnnotation() || afterGroupsAnnotation() || beforeClassAnnotation() || afterClassAnnotation() || beforeMethodAnnotation() || afterMethodAnnotation() {
        try {
            proceed();
        } catch (Throwable ex) {
            System.out.println("logging exception: " + ex);
        }
    }
}