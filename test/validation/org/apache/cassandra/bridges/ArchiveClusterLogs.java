/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.cassandra.bridges;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.cassandra.bridges.ccmbridge.CCMBridge;

public class ArchiveClusterLogs
{
    public static boolean checkForFolder(String dirPath)
    {
        File file = new File(dirPath);
        return file.exists();
    }

    public static void zipExistingDirectory(String existDir)
    {
        String sourceFolderName =  existDir;
        long unixTime = System.currentTimeMillis() / 1000L;
        String outputFileName = existDir + "_" + unixTime + ".zip";

        try
        {
            FileOutputStream fos = new FileOutputStream(outputFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.setLevel(9);
            compressFiles(zos, sourceFolderName, sourceFolderName);
            zos.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void compressFiles(ZipOutputStream zos,String folderName,String baseFolderName)
    {
        File f = new File(folderName);

        try
        {
            if (f.exists())
            {
                if (f.isDirectory())
                {
                    File f2[] = f.listFiles();
                    for (int i = 0; i < f2.length; i++)
                    {
                        compressFiles(zos, f2[i].getAbsolutePath(), baseFolderName);
                    }
                }
                else
                {
                    String entryName = folderName.substring(baseFolderName.length() + 1, folderName.length());
                    ZipEntry ze = new ZipEntry(entryName);
                    zos.putNextEntry(ze);
                    FileInputStream in = new FileInputStream(folderName);
                    int len;
                    byte buffer[] = new byte[1024];
                    // Reads up to 1024 bytes of data from the file
                    while ((len = in.read(buffer)) > 0)
                    {
                        // Writes the data to the current ZipEntry
                        zos.write(buffer, 0, len);
                    }
                    in.close();
                    zos.closeEntry();
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void savetempDirectoryPath(File CASSANDRA_DIR, File tmp_Dir)
    {
        File filePath = new File(CASSANDRA_DIR + "/build/test/logs/validation/tempDir.txt");

        try
        {
            if(!filePath.exists())
            {
                filePath.getParentFile().mkdirs();
            }
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
            pw.println(tmp_Dir);
            pw.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void removeOldCluster(File CASSANDRA_DIR)
    {
        File filePath = new File(CASSANDRA_DIR + "/build/test/logs/validation/tempDir.txt");

        if (filePath.exists())
        {
            try
            {
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                String line = br.readLine();
                String tmpDir = null;

                while(line != null){

                    tmpDir = line;
                    line = br.readLine();
                }

                File tempDir = new File(tmpDir);
                if (tempDir.exists())
                {
                    if(tempDir.list().length > 0)
                    {
                        CCMBridge.removeLiveCluster(tmpDir);
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
