# GeorgesRahRepo
This File will help you migrate your code from Ibatis to Mybatis with the minimum changes required from your side
What you have to do is to locate the java class in your package and run the class 
    public static void main(String[] args)
    {
        final String folder_path = "D://SVN_PROJECTS//Fullpackage"; // specify your package here 
        ConvertIbatisToMybatis c = new ConvertIbatisToMybatis();
        c.loopFilesinFolder(folder_path);
    }
    
    The java class will loop all xml files located under this path and create a backup for each on the same location and do the migration 
    The migration covers the essential steps as well as the generattion of all typeAlias that should be copied to the configuration 
    
    
    
 
