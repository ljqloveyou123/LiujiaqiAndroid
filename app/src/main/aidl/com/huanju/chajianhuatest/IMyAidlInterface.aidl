// IMyAidlInterface.aidl
package com.huanju.chajianhuatest;
import com.huanju.chajianhuatest.aidlmode.TestBean;
// Declare any non-default types here with import statements

interface IMyAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

     String getS(in TestBean s);

     TestBean getInfoBean(inout TestBean b);

}
