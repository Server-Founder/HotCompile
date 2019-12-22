package net.noyark.www.hotcompile

import java.io.File
import java.util

import cn.nukkit.Server
import cn.nukkit.plugin.PluginBase
import javax.tools.ToolProvider
import net.noyark.www.hotcompile.devjar.DevJar
import org.apache.commons.io.FileUtils

object JavaCompiler {

    private val pluginsFiles:util.ArrayList[File] = new util.ArrayList[File]
    private val compiler = ToolProvider.getSystemJavaCompiler

    private var classPath = ""

    //javac -classpath lib/jxl.jar -sourcepath src @sourcelist.txt -d bin

    private def getFilePaths(file: File,arrayList: util.ArrayList[String]): Unit ={
      var files = file.listFiles()
      if(files!=null)
      for(f <- files){
        if(f.isFile&&f.toString.endsWith(".java")){
          arrayList.add(f.toString)
        }else{
          getFilePaths(f,arrayList)
        }
      }
    }

    def simpleCompile(file: File): Unit ={
        val makeFile = new File(file+"/makeFile.txt")
        val list = new util.ArrayList[String]()
        val out = new File(file.getParent+"/"+file.getName+".out")
        if(!out.exists()){
          out.mkdirs()
        }
        getFilePaths(file,list)
        getDepend()
        FileUtils.writeLines(makeFile,list,false)
        compiler.run(null,null,null,"-classpath",classPath,
            "-sourcepath","src","@"+makeFile,"-d",out.toString
        )
    }

  private def getDepend(): Unit ={
    if(pluginsFiles.isEmpty){
      val method = classOf[PluginBase].getDeclaredMethod("getFile")
      Server.getInstance().getPluginManager.getPlugins.entrySet().stream().forEach(
        x=>{
          pluginsFiles.add(method.invoke(x.getValue.asInstanceOf[PluginBase]).asInstanceOf[File])
        }
      )
      dependString()
    }
  }
  //-classpath
  private def dependString(): Unit ={
    if(classPath.equals("")){
      val builder = new StringBuilder(classPath)
      pluginsFiles.stream().forEach(x=>{
        if(pluginsFiles.size()!=1) builder.append(x).append(File.separator.replace("/",":").replace("\\",";")) //linux : windows ;
        else builder.append(x)
      })
    }

  }

  //javac -encoding utf-8 -d .\target .\src\main\java\com\rui\*.java

    def getPluginYaml(files:Array[File]): File={
      for(file<-files){
        if(file.getName.equals("plugin.yml")){
          return file
        }
      }
      null
    }


    def hotCompile(f: File,plugin:PluginBase) : Unit = {
      JavaCompiler simpleCompile new File(f+"/src")
      val outRoot = JavaCompiler getOutRootPath f
      val jarFile = plugin.getDataFolder.getParentFile+outRoot.getName+".hotcompile.jar"
      DevJar.devJar(outRoot.toString,jarFile) //打包
      plugin.getPluginLoader.loadPlugin(jarFile) //加载
    }

    def getClassPath(file:File,rootFile:File)=file.toString.replace(rootFile.toString,"")

    def getOutPath(file:File,root : File) = getOutRootPath(root)+getClassPath(file,root)

    def getOutRootPath(root: File) = new File(HotCompile.getHotCompile.getDataFolder+"/"+root.getName+".out")

}
