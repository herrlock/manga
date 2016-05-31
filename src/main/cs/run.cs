using Microsoft.Win32;
using System;
namespace Tasks_Playground
{
    static class MangaLauncher
    {
        [STAThread]
        static void Main()
        {
            string mgaFile = "MangaLauncher.jar";
            string currentJavaVersion = Registry.LocalMachine.OpenSubKey("SOFTWARE\\JavaSoft\\Java Runtime Environment").GetValue("CurrentVersion").ToString();
            if (System.IO.File.Exists(mgaFile))
            {
                System.Diagnostics.Process mangaLounch = new System.Diagnostics.Process();
                mangaLounch.StartInfo.UseShellExecute = false;
                mangaLounch.StartInfo.FileName = "java";
                mangaLounch.StartInfo.Arguments = " -jar " + mgaFile;
                try
                {
                    mangaLounch.Start();
                }
                catch (Exception ec)
                {
                    System.Windows.Forms.MessageBox.Show("Application can not runn on Java " + currentJavaVersion + "!", "Application can not runn, maybe java is not installed.");
                }
            }
            else
            {
                System.Windows.Forms.MessageBox.Show("MangaLauncher not found.", "Application can not runn, maybe the java pack is not in current directory.");
            }
        }
    }
}
