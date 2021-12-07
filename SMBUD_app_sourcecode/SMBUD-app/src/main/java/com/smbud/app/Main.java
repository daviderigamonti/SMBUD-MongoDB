package com.smbud.app;

import com.smbud.app.db.Verify;

public class Main {
    public static void main(String[] args) {

        Verify.start();
        GUI.run(args);

    }
}
