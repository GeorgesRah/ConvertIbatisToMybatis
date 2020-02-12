package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

public class ConvertIbatisToMybatis
{

    private boolean isFolderExists(String path)
    {
        File f = new File(path);
        if (!f.exists())
        {
            System.out.println("The Path you entered is wrong. Please Change");
            return false;
        }

        if (!f.isDirectory())
        {
            System.out.println("The Path you entered does not lookup to a folder. Please Change");
            return false;
        }
        return true;
    }

    private void duplicateFile(String originalFile, String newFile)
        throws IOException
    {
        InputStream is = null;
        OutputStream os = null;
        try
        {
            is = new FileInputStream(originalFile);
            os = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0)
            {
                os.write(buffer, 0, length);
            }
        }
        finally
        {
            is.close();
            os.close();
        }
    }

    private void loopFilesinFolder(String folder_path)
    {
        File dir = new File(folder_path);
        isFolderExists(folder_path);
        File[] directoryListing = dir.listFiles();

        String new_path = folder_path;
        String file_ext = null;

        try
        {
            if (directoryListing != null)
            {
                for (File child : directoryListing)
                {

                    if (child.isDirectory())
                    {
                        new_path = child.getPath();
                        System.out.println("\n\nA new Folder is found under the following path:" + child.getName());
                        loopFilesinFolder(new_path);
                    }
                    else if (child.isFile())
                    {
                        // test to identify if the found file is an xml file
                        // if the file is an xml it should be duplicated and the
                        // old
                        // file renamed to name.xml_bck
                        file_ext = FilenameUtils.getExtension(child.getName());
                        if (file_ext != null && file_ext != "" && file_ext.toLowerCase().equals("xml"))
                        {
                            System.out.println("XML File found with name : " + child.getName());
                            String originalFile = child.getParent() + File.separator + child.getName();
                            String newFile = child.getParent() + File.separator + child.getName().concat("_bck");
                            // create a backup file
                            duplicateFile(originalFile, newFile);
                            System.out.println("XML backup File is created with name : " + child.getName() + "_bck");

                            migrateFileToMybatis(originalFile);
                        }
                    }
                }
            }
            else
            {
                return;
            }
        }
        catch (Exception e)
        {

        }
    }

    private void migrateFileToMybatis(String file)
    {
        try
        {
            doTheReplace(file);
        }
        catch (Exception e)
        {
        }
    }

    private void doTheReplace(String filePath)
        throws FileNotFoundException, IOException
    {
        List<String> replaced = null;
        try
        {
            Path path = Paths.get(filePath);
            Stream<String> lines = Files.lines(path);
            replaced = lines.map(line -> migrateSyntax(line)).collect(Collectors.toList());

            Files.write(path, replaced);
            lines.close();
            System.out.println("Migration to MYBATIS is done!!!");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String migrateSyntax(String line)
    {
        String ibatisDef = "<!DOCTYPE sqlMap PUBLIC \"-//iBATIS.com//DTD SQL Map 2.0//EN\" \"http://www.ibatis.com/dtd/sql-map-2.dtd\">";
        String myBatisDef = "<!DOCTYPE configuration PUBLIC \"-//mybatis.org//DTD Config 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-config.dtd\">";

        if (line.startsWith("<typeAlias"))
        {
            System.out.println(line);
            return "";
        }

        line = line.replaceAll(ibatisDef, myBatisDef);
        // Add all the replace here !!!

        line = line.replaceAll("<sqlMap", "<mapper");
        line = line.replaceAll("<result property=", "<association property=");

        line = line.replaceAll("parameterClass", "parameterType");
        line = line.replaceAll("resultClass", "resultType");
        line = line.replaceAll("class", "type");
        line = line.replaceAll("<iterate ", "<foreach ");
        line = line.replaceAll("</iterate>", "</foreach>");

        line = line.replaceAll(":NUMERIC#", ",jdbcType=NUMERIC#");
        line = line.replaceAll(":NUMBER#", ",jdbcType=NUMBER#");
        line = line.replaceAll(":VARCHAR#", ",jdbcType=VARCHAR#");
        line = line.replaceAll(":BLOB#", ",jdbcType=BLOB#");
        line = line.replaceAll(":DATE#", ",jdbcType=DATE#");
        line = line.replaceAll(":TIMESTAMP#", ",jdbcType=TIMESTAMP#");

        line = line.replaceAll("</sqlMap>", "</mapper>");

        // replace $parameter$ by ${parameter}
        line = replaceAllDollarRegEX(line);
        // replace #parameter# by #{parameter}
        line = replaceAllHashTagRegEX(line);

        // replace isNotNull by if condition
        line = replaceNotNullConditionsByIfStatement(line);
        line = line.replaceAll("</isNotNull>", "</if>");

        // replace isNull by if condition
        line = replaceNullConditionsByIfStatement(line);
        line = line.replaceAll("</isNull>", "</if>");

        // replace isEqual by if condition
        line = replaceEqualConditionsByIfStatement(line);
        line = line.replaceAll("</isEqual>", "</if>");

        // replace isNotEqual by if condition
        line = replaceNotEqualConditionsByIfStatement(line);
        line = line.replaceAll("</isNotEqual>", "</if>");

        return line;
    }

    private String replaceAllHashTagRegEX(String line)
    {
        String REGEX = "#([a-zA-Z0-9,_.=\\[\\]]{2,})#";
        String REPLACE = "#{$1}";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        return line;
    }

    private String replaceAllDollarRegEX(String line)
    {
        String REGEX = "\\$([a-zA-Z0-9,_.=\\[\\]]{2,})\\$";
        String REPLACE = "\\${$1}";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        return line;
    }

    private String replaceNotNullConditionsByIfStatement(String line)
    {
        String REGEX = "<isNotNull property=\"([a-zA-Z0-9,_.=\\[\\]]{2,})\"";
        String REPLACE = "<if test=\"$1 != null\"";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        System.out.println("line=" + line);
        return line;
    }

    private String replaceNullConditionsByIfStatement(String line)
    {
        String REGEX = "<isNull property=\"([a-zA-Z0-9,_.=\\[\\]]{2,})\"";
        String REPLACE = "<if test=\"$1 == null\"";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        System.out.println("line=" + line);
        return line;
    }

    private String replaceEqualConditionsByIfStatement(String line)
    {
        String REGEX = "<isEqual property=\"([a-zA-Z0-9,_.=\\[\\]]{2,})\" ";
        String REPLACE = "<if test=\"$1 == DAM\" ";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        System.out.println("line=" + line);

        REGEX = "DAM\" compareValue=\"([a-zA-Z0-9,_.=\\[\\]]{2,})\"";
        REPLACE = "$1\"";
        // get a matcher object
        p = Pattern.compile(REGEX);
        m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        System.out.println("line=" + line);

        return line;
    }

    private String replaceNotEqualConditionsByIfStatement(String line)
    {
        String REGEX = "<isNotEqual property=\"([a-zA-Z0-9,_.=\\[\\]]{2,})\" ";
        String REPLACE = "<if test=\"$1 == DAM\" ";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        System.out.println("line=" + line);

        REGEX = "DAM\" compareValue=\"([a-zA-Z0-9,_.=\\[\\]]{2,})\"";
        REPLACE = "$1\"";
        // get a matcher object
        p = Pattern.compile(REGEX);
        m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        System.out.println("line=" + line);

        return line;
    }

    public static void main(String[] args)
    {
        final String folder_path = "D://Projects//Suite//project//com//dao//";

        ConvertIbatisToMybatis c = new ConvertIbatisToMybatis();
        System.out.println("Start ConvertIbatisToMybatis... ");
    }

}
