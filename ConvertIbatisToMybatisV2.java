package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
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

    boolean isPreviousLineTypeAliasFound = false;

    boolean isPreviousLineIteratorFound = false;
    boolean isPreviousLineSqlMapCalled = false;

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

    private void revertXmlMigration(String folder_path)
    {
        System.out.println("revertMigration started ... ");
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
                        // System.out.println("\n\nA new Folder is found under
                        // the following path:" + child.getName());
                        revertXmlMigration(new_path);
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
                            child.delete();
                        }
                        else if (file_ext != null && file_ext != "" && file_ext.toLowerCase().equals("xml_bck"))
                        {
                            File f1 = new File(child.getPath());
                            File f2 = new File(child.getPath().replace(".xml_bck", ".xml"));
                            boolean b = f1.renameTo(f2);
                        }
                    }
                }
            }
            else
            {
                return;
            }

            System.out.println("revertMigration ended ... ");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private void migrateXmlFiles(String folder_path)
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
                        // System.out.println("\n\nA new Folder is found under
                        // the following path:" + child.getName());
                        migrateXmlFiles(new_path);
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
                            // System.out.println("XML File found with name : "
                            // + child.getName());
                            String originalFile = child.getParent() + File.separator + child.getName();
                            String newFile = child.getParent() + File.separator + child.getName().concat("_bck");
                            // create a backup file
                            File newFileDup = new File(newFile);
                            if (newFileDup.exists())
                            {
//                                System.out.println("File Already Exists : " + newFile);
                            }
                            else
                            {
                                duplicateFile(originalFile, newFile);
                            }
                            // System.out.println("XML backup File is created
                            // with name : " + child.getName() + "_bck");

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
            // System.out.println("Migration to MYBATIS is started!!!\n\n");
            Path path = Paths.get(filePath);
            Stream<String> lines = Files.lines(path, Charset.forName("Cp1252"));
            replaced = lines.map(line -> migrateSyntax(line)).collect(Collectors.toList());

            Files.write(path, replaced);
            lines.close();
            // System.out.println("\n\nMigration to MYBATIS is done!!!");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String migrateSyntax(String line)
    {

        String ibatisDef = "<!DOCTYPE sqlMap PUBLIC \"-//iBATIS.com//DTD SQL Map 2.0//EN\" \"http://www.ibatis.com/dtd/sql-map-2.dtd\">";
        String myBatisDef = "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">";

        if (line.contains("<typeAlias"))
        {
            System.out.println(line);
            isPreviousLineTypeAliasFound = true;
            if(line.contains("/>"))
                isPreviousLineTypeAliasFound = false;
            return "";
        }
        if (isPreviousLineTypeAliasFound)
        {
            System.out.println(line);
            if(line.contains("/>"))
                isPreviousLineTypeAliasFound = false;
            return "";
        }
        isPreviousLineTypeAliasFound = false;

        line = line.replaceAll(ibatisDef, myBatisDef);
        // Add all the replace here !!!

        line = line.replaceAll("<sqlMap", "<mapper");
        // line = line.replaceAll("<result property=", "<association
        // property=");
        line = line.replaceAll("<procedure", "<update statementType=\"CALLABLE\"");
        line = line.replaceAll("</procedure>", "</update>");

        line = line.replaceAll("remapResults=\"true\"", "");
        line = line.replaceAll("parameterClass", "parameterType");
        line = line.replaceAll("resultClass", "resultType");
        line = line.replaceAll(" class=", " type=");
        line = line.replaceAll("\tclass=", "\ttype=");
        line = line.replaceAll(" class =", " type =");
        line = line.replaceAll(" class = ", " type = ");
        line = line.replaceAll("\tclass = ", "\ttype = ");
        line = line.replaceAll("\tclass =", "\ttype =");
        line = line.replaceAll("property = ", "property=");
        line = line.replaceAll("property =", "property=");

        line = line.replaceAll(":NUMERIC#", ",jdbcType=NUMERIC#");
        line = line.replaceAll(":INTEGER#", ",jdbcType=NUMERIC#");
        line = line.replaceAll(":NUMBER#", ",jdbcType=NUMERIC#");
        line = line.replaceAll(":VARCHAR#", ",jdbcType=VARCHAR#");
        line = line.replaceAll(":BLOB#", ",jdbcType=BLOB#");
        line = line.replaceAll(":DATE#", ",jdbcType=DATE#");
        line = line.replaceAll(":TIMESTAMP#", ",jdbcType=TIMESTAMP#");
        line = line.replaceAll("jdbcType=\"NUMBER\"", "jdbcType=\"NUMERIC\"");

        line = line.replaceAll("</sqlMap>", "</mapper>");
        line = line.replace("<selectKey", "<selectKey order=\"BEFORE\"");

        // replace dynamic by WHERE
        line = line.replaceAll("<dynamic prepend=\"where\">", "<where>");
        line = line.replaceAll("</dynamic", "</where");

        // replace prepend
        line = replacePrepend(line);

        // replace $parameter$ by ${parameter}
        line = replaceAllDollarRegEX(line);
        // replace #parameter# by #{parameter}
        line = replaceAllHashTagRegEX(line);

        // replace isNotNull by if condition
        line = replaceNotNullConditionsByIfStatement(line);
        line = line.replaceAll("</isNotNull", "</if");

        // replace isNull by if condition
        line = replaceNullConditionsByIfStatement(line);
        line = line.replaceAll("</isNull", "</if");

        // replace isNotEmpty by if condition
        line = replaceNotEmptyConditionsByIfStatement(line);
        line = line.replaceAll("</isNotEmpty", "</if");

        // replace isEmpty by if condition
        line = replaceEmptyConditionsByIfStatement(line);
        line = line.replaceAll("</isEmpty", "</if");

        // replace isEqual by if condition
        line = replaceEqualNumberConditionsByIfStatement(line);
        line = replaceEqualConditionsByIfStatement(line);
        line = line.replaceAll("</isEqual", "</if");

        // replace isNotEqual by if condition
        line = replaceNotEqualNumberConditionsByIfStatement(line);
        line = replaceNotEqualConditionsByIfStatement(line);
        line = line.replaceAll("</isNotEqual", "</if");

        // replace iterate by foreach
        line = replaceIterateByForeach(line);
        line = replaceIteratePropertyByItem(line);
        line = line.replaceAll("</iterate", "</foreach");

        return line;
    }

    private String replacePrepend(String line)
    {

        if (line.toLowerCase().indexOf("prepend =") != -1)
            line = line.replaceAll("prepend =", "prepend=");
        
        if (line.toLowerCase().indexOf(" prepend=\"and\"") != -1)
            line = line.replaceAll(" prepend=\"and\"", "").replaceAll(" prepend=\"And\"", "")
                    .replaceAll(" prepend=\"AND\"", "").replaceFirst(">", "> and ");

        if (line.toLowerCase().indexOf(" prepend=\"where\"") != -1)
            line = line.replaceAll(" prepend=\"where\"", "").replaceAll(" prepend=\"Where\"", "")
                    .replaceAll(" prepend=\"WHERE\"", "").replaceFirst(">", "> where ");

        if (line.toLowerCase().indexOf(" prepend=\"union all\"") != -1)
            line = line.replaceAll(" prepend=\"union all\"", "").replaceAll(" prepend=\"union All\"", "")
                    .replaceAll(" prepend=\"UNION ALL\"", "").replaceFirst(">", "> union all ");

        return line;
    }

    private String replaceAllHashTagRegEX(String line)
    {
        String REGEX = "#([a-zA-Z0-9,_\\-.=\\[\\]]{2,})#";
        String REPLACE = "#{$1}";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceAllHashTagRegEX =" + line);
        return line;
    }

    private String replaceAllDollarRegEX(String line)
    {
        String REGEX = "\\$([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\\$";
        String REPLACE = "\\${$1}";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceAllDollarRegEX =" + line);
        return line;
    }

    private String replaceNotNullConditionsByIfStatement(String line)
    {
        String REGEX = "<isNotNull(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"";
        String REPLACE = "<if test=\"$2 != null\"";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceNotNullConditionsByIfStatement
        // =" + line);
        return line;
    }

    private String replaceNullConditionsByIfStatement(String line)
    {
        String REGEX = "<isNull(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"";
        String REPLACE = "<if test=\"$2 == null\"";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceNullConditionsByIfStatement ="
        // + line);
        return line;
    }

    private String replaceNotEmptyConditionsByIfStatement(String line)
    {
        String REGEX = "<isNotEmpty(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"";
        String REPLACE = "<if test=\"$2 != null and $2 != ''\"";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceNotEmptyConditionsByIfStatement
        // =" + line);
        return line;
    }

    private String replaceEmptyConditionsByIfStatement(String line)
    {
        String REGEX = "<isEmpty(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"";
        String REPLACE = "<if test=\"$2 == null or $2 == ''\"";

        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceEmptyConditionsByIfStatement ="
        // + line);
        return line;
    }

    private String replaceEqualNumberConditionsByIfStatement(String line)
    {

        String REGEX = "<isEqual(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"(.*)compareValue=\"([0-9]{1,})\"";
        String REPLACE = "<if test=\"$2 != null and $2 == $4\" ";
        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
//         System.out.println("line after replaceEqualConditionsByIfStatement ="+ line);
        return line;
    }
    
    private String replaceEqualConditionsByIfStatement(String line)
    {
        
        String REGEX = "<isEqual(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"(.*)compareValue=\"([a-zA-Z0-9,&; _\\-.=\\[\\]]{0,})\"";
        String REPLACE = "<if test=\"$2 != null and $2 == '$4'\" ";
        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
//         System.out.println("line after replaceEqualConditionsByIfStatement ="+ line);
        return line;
    }

    private String replaceNotEqualNumberConditionsByIfStatement(String line)
    {
        String REGEX = "<isNotEqual(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"(.*)compareValue=\"([0-9]{1,})\"";
        String REPLACE = "<if test=\"$2 != null and $2 != $4\" ";
        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceNotEqualConditionsByIfStatement
        // =" + line);

        return line;
    }
    
    private String replaceNotEqualConditionsByIfStatement(String line)
    {
        String REGEX = "<isNotEqual(.*) property=\"([a-zA-Z0-9,_\\-.=\\[\\]]{2,})\"(.*)compareValue=\"([a-zA-Z0-9,&; _\\-.=\\[\\]]{0,})\"";
        String REPLACE = "<if test=\"$2 != null and $2 != '$4'\" ";
        // get a matcher object
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(line);
        line = m.replaceAll(REPLACE);
        // System.out.println("line after replaceNotEqualConditionsByIfStatement
        // =" + line);
        
        return line;
    }

    private String replaceIterateByForeach(String line)
    {

        String moreAttributes = "";
        if (line.indexOf("<iterate") != -1)
        {
            line = line.replace(" property", " collection");
            line = line.replace(" conjunction", " separator");
            if (line.indexOf(" collection") == -1)
            {
                moreAttributes += " collection=\"list\"";
            }
            line = line.replace("<iterate", "<foreach item=\"item\" index=\"index\" " + moreAttributes);
        }
//         System.out.println("line after replaceIterateByForeach =" + line);
        return line;

    }

    private String replaceIteratePropertyByItem(String line)
    {
        if (line.indexOf("[]") != -1)
        {
            String REGEX = "([\\$#]){1}\\{([a-zA-Z0-9,_\\-.=]{2,}\\[\\])";
            String REPLACE = "$1{item";
            // get a matcher object
            Pattern p = Pattern.compile(REGEX);
            Matcher m = p.matcher(line);
            line = m.replaceAll(REPLACE);
        }
//         System.out.println("line after replaceIteratePropertyByItem =" + line);
        return line;
    }
    
    private void revertDAOMigration(String folder_path)
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
                        revertDAOMigration(new_path);
                    }
                    else if (child.isFile())
                    {
                        if (child.getName().indexOf("DAOImpl.java") != -1)
                        {
                            file_ext = FilenameUtils.getExtension(child.getName());
                            if (file_ext != null && file_ext != "" && file_ext.toLowerCase().equals("java"))
                            {
                                child.delete();
                            }
                            else if (file_ext != null && file_ext != "" && file_ext.toLowerCase().equals("java_bck"))
                            {
                                File f1 = new File(child.getPath());
                                File f2 = new File(child.getPath().replace(".java_bck", ".java"));
                                boolean b = f1.renameTo(f2);
                            }
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
    private void updateDAOImpl(String folder_path)
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
                        updateDAOImpl(new_path);
                    }
                    else if (child.isFile())
                    {
                        if ((child.getName().indexOf("DAOImpl.java") != -1) && (child.getName().indexOf("DAOImpl.java_bck") == -1))
                        {
                            String originalFile = child.getParent() + File.separator + child.getName();
                            String newFile = child.getParent() + File.separator + child.getName().concat("_bck");
                            File newFileDup = new File(newFile);
                            if (newFileDup.exists())
                            {
//                                System.out.println("File Already Exists : " + newFile);
                            }
                            else
                            {
                                duplicateFile(originalFile, newFile);
                            }
                            migrateDAOFileToMybatis(originalFile);
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
    
    private void migrateDAOFileToMybatis(String filePath)
    {
        List<String> replaced = null;
        try
        {
//            System.out.println("Migration of all DAO files is started!!!\n\n");
            Path path = Paths.get(filePath);
            Stream<String> lines = Files.lines(path, Charset.forName("Cp1252"));
            replaced = lines.map(line -> migrateDAOSyntax(line)).collect(Collectors.toList());

            Files.write(path, replaced);
            lines.close();
//            System.out.println("\n\nMigration of all DAO files is done!!!");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private String migrateDAOSyntax(String line)
    {
        line = line.replaceAll("getSqlMap\\(\\).queryForList", "selectList");
        line = line.replaceAll("getSqlMap\\(\\).queryForObject", "getSqlSessionTemplate().selectOne");
        line = line.replaceAll("getSqlMap\\(\\).queryForMap", "getSqlSessionTemplate().selectMap");
        line = line.replaceAll("sqlMap.queryForList", "selectList");
        line = line.replaceAll("sqlMap.queryForObject", "getSqlSessionTemplate\\(\\).selectOne");
        line = line.replaceAll("sqlMap.queryForMap", "getSqlSessionTemplate\\(\\).selectMap");

        if((line.indexOf("fixGridMaps") == -1) && ((line.indexOf("getSqlMap()")!= -1 && line.indexOf("getSqlMap().")== -1)
                ||(line.indexOf("sqlMap")!= -1 && line.indexOf("sqlMap.")== -1)))
        {
            line = line.replace("getSqlMap()","");
            line = line.replace("sqlMap","");
        }

        line = line.replaceAll(" .queryForList", "selectList");
        line = line.replaceAll(" .queryForObject", "getSqlSessionTemplate\\(\\).selectOne");
        
        if(line.indexOf(".startBatch") != -1 || line.indexOf(".executeBatch") != -1 
                || line.indexOf(".startTransaction()") != -1 || line.indexOf(".getCurrentConnection()") != -1 
                || line.indexOf("con.setAutoCommit") != -1 || line.indexOf("con.commit") != -1 
                || line.indexOf(".getCurrentConnection()") != -1 
                || line.indexOf(".endTransaction()") != -1 )
        {
            line = "//"+line;
        }
        if(line.indexOf("SQLException")!= -1)
        {
            line.replace("SQLException", "Exception");
        }
        
        line = line.replaceAll("getSqlMap\\(\\)","getSqlSessionTemplate\\(\\)");
        line = line.replaceAll("sqlMap","getSqlSessionTemplate\\(\\)");
        
        return line;
    }
    public static void main(String[] args)
    {
        final String folder_path = "D://SVN_Projects//Valoores_Suite//project//Valoores_Suite//javasource//com//softsolutions//col//dao//";

        ConvertIbatisToMybatis c = new ConvertIbatisToMybatis();
        System.out.println("Start ConvertIbatisToMybatis... ");
        
        // Revert the Changes of XML
        c.revertXmlMigration(folder_path);
        
        // Migration of XML
        c.migrateXmlFiles(folder_path);
        
        // Revert the Changes of DAOImpl
        c.revertDAOMigration(folder_path);
        
        // Migration of DAOImpl
        c.updateDAOImpl(folder_path);

        System.out.println("End ConvertIbatisToMybatis... ");

    }

}
