/**
*
* @author OKTAY TÜRKMEN, koteybeh.mohemmed@ogr.sakarya.edu.tr
* @since 06.04.2024
* <p>
* Kullanıcıdan GitHub deposu URL'sini alır. oradaki depoyu belirtilen bir yerel klasöre klonlar.
* Klonlanmış Java dosyalarını Analyzer sınıfıyla analiz eder ve Analiz sonuçlarını ekrana yazdırır.
* </p>
*/
package G201210557;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("GitHub deposu URL'sini girin: ");
        String repoUrl = scanner.nextLine();
        String localPath = "depo"; // Yeni bir klasör adı verin
        Analyzer.cloneRepository(repoUrl, localPath);
        
     //.java uzantılı dosyaları liste olarak döndürür ve bu listeyi javaFiles adlı değişkende saklar. 
        List<Path> javaFiles = Analyzer.listJavaFiles(localPath);
  
       //her bir Java dosyası için Analyzer sınıfının analyzeJavaFile metodunu çağrarak  dosya analiz edilir ve sonuçlar ekrana yazdırılır.
        for (Path javaFile : javaFiles) {
            Analyzer.analyzeJavaFile(javaFile);
        }

        scanner.close();
    }

}
