package com.joking.incrementalupdate;
/*
 * BsPatch     2017-05-23
 * Copyright (c) 2017 JoKing All right reserved.
 */

public class BsPatch {
    /**
     * 合并
     *
     * @param oldfile
     * @param newfile
     * @param patchfile
     */
    public native static void patch(String oldfile, String newfile, String patchfile);

    static {
        System.loadLibrary("update-lib");
    }
}
