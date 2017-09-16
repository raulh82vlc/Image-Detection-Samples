/*
 * Copyright (C) 2017 Raul Hernandez Lopez @raulh82vlc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.raulh82vlc.image_recognition_sample.opencv.domain;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File Helper has methods for File manipulation
 * @author Raul Hernandez Lopez.
 */
public final class FileHelper {

    private FileHelper() {

    }

    /**
     * Reads a Cascade file from a raw resource and returns the {@link File}
     */
    public static File readCascadeFile(Context context, int rawFile, String dir,
                                       String fileOutput) throws IOException {
        // load cascade file from application resources
        InputStream is = context.getResources().openRawResource(rawFile);
        File cascadeDir = context.getDir(dir, Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, fileOutput);
        FileOutputStream os = new FileOutputStream(cascadeFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
        return cascadeFile;
    }
}
