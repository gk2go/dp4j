package com.dp4j;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    private static AppTest instance = new AppTest();

    /**
     * We need one test so that Maven will happily run the test phase
     */
    @Test
    public void dummyTest() {
        //        File srcFile = new File("C:\Users\simpatico\Downloads\AnnotationProcessorMessagerBug\DP4Java\src\test\java\com\mysimpatico\se\dp4java\annotations\SingletonImpl.java");
        //            if(!srcFile.exists()){
        //                throw new RuntimeException();
        //            }
        //            FileObject fileObj = FileUtil.toFileObject(srcFile);
        //            final JavaSource classSource = JavaSource.forFileObject(fileObj);
//        SS sS = new SS();
//        sS.index = 4;
        try{
        AppTest pp = AppTest.instance;
//        SingletonImpl ss = SingletonImpl.instance;
        }catch(Exception e){
            
        }
        System.out.println("hello from dummy test");
    }
}
