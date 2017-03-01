package com.huanju.chajianhuatest.aidlmode;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by DELL-PC on 2017/2/20.
 */

public class TestBean implements Parcelable{
    public String s;
    public int i;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.s);
        dest.writeInt(this.i);
    }
    public void readFromParcel(Parcel dest) {
        s = dest.readString();
        i = dest.readInt();
    }
    public TestBean() {
    }
    protected TestBean(Parcel in) {
        this.s = in.readString();
        this.i = in.readInt();
    }

    public static final Creator<TestBean> CREATOR = new Creator<TestBean>() {
        @Override
        public TestBean createFromParcel(Parcel source) {
            return new TestBean(source);
        }

        @Override
        public TestBean[] newArray(int size) {
            return new TestBean[size];
        }
    };
}
