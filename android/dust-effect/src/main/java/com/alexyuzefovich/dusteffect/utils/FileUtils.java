package com.alexyuzefovich.dusteffect.utils;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Alexander Yuzefovich
 * */
public class FileUtils {

    @NonNull
    public static String readTextFromRaw(@NonNull Context context, int resourceId) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = null;
            try {
                InputStream inputStream = context.getResources().openRawResource(resourceId);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\r\n");
                }
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
        } catch (IOException | Resources.NotFoundException ex) {
            ex.printStackTrace();
        }
        return stringBuilder.toString();
    }

}
