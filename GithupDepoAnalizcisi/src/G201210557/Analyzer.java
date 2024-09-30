/**
*
* @author OKTAY TÜRKMEN, koteybeh.mohemmed@ogr.sakarya.edu.tr
* @since 06.04.2024
* <p>
* Bu sınıf, klonlanmış Java dosyalarının analizini gerçekleştirir. 
* listJavaFiles metodu, verilen bir klasör yolundaki tüm Java dosyalarını liste olarak döndürür. 
* analyzeJavaFile metodu, her bir Java dosyasını analiz eder belirli metrikleri hesaplar .
* </p>
*/

package G201210557;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;


public class Analyzer {

    private static final Pattern CLASS_DESENİ = Pattern.compile("class\\s+([\\w\\d]+)\\s*");

    public static void cloneRepository(String repoUrl, String localPath) {
        try {
        	// Eğer 'depo' klasörü daha önceden varsa, sil
            if (Files.exists(Paths.get(localPath))) {
                deleteFolder(Paths.get(localPath));
            }
          // Yeni 'depo' klasörünü oluştur
            Files.createDirectories(Paths.get(localPath));

            ProcessBuilder builder = new ProcessBuilder("git", "clone", repoUrl, localPath);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            if (listJavaFiles(localPath).isEmpty()) {
                System.out.println("verilen url'de .java uzantılı dosyalar bulunamadı.");
                deleteFolder(Paths.get(localPath));
            } else {
                System.out.println();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Git reposu klonlanırken bir hata oluştu:");
            e.printStackTrace();
        }
    }
    //bu metod verilen bir klasörü içindeki tüm dosya ve alt klasörleri ile birlikte siler.
    private static void deleteFolder(Path folder) throws IOException {
        Files.walk(folder)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
      //verilen bir klasör yolundaki tüm Java dosyalarını liste olarak döndürür.
    public static List<Path> listJavaFiles(String localPath) {
        List<Path> javaFiles = new ArrayList<>();
        try {
            Files.walk(Paths.get(localPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(javaFiles::add);
        } catch (IOException e) {
            System.err.println("Java dosyaları listelenirken bir hata oluştu:");
            e.printStackTrace();
        }
        return javaFiles;
    }

    public static void analyzeJavaFile(Path javaFile) {
        try {
            if (!Files.exists(javaFile)) {
                System.err.println("Dosya bulunamadı: " + javaFile);
                return;
            }
            List<String> lines = Files.readAllLines(javaFile);
            String className = getClassName(lines);
            if (className == null) {
            	//System.err.println("Sınıf adı bulunamadı: " + javaFile.getFileName());
                return;
            }

            long javaDocComments = countjavaDocComments(lines);
            long singleLineComments = countSingleLineComments(lines);
            long multiLineComments = countMultiLineComments(lines);
            long otherComments = singleLineComments + multiLineComments;// Diğer Yorum Satırı Sayısı
            long codeLines = countCodeLines(lines);
            long loc = lines.size();// LOC (Line of Code) hesaplaması


            long functions = countFunctions(lines);

            double YG = ((javaDocComments + otherComments) * 0.8) / functions;
            double YH = (((double) codeLines / functions) * 0.3);
            double comDeviation = ((100 * YG) / YH) - 100;
            double rounded = Math.round(comDeviation * 100.0) / 100.0;
            
            
            // Analiz sonuçlarını yazdırma
            System.out.println();
            System.out.println("------------------------------------------------");
            //System.out.println();
            System.out.println("Sınıf: " + javaFile.getFileName());
            System.out.println("Javadoc Satırı Sayısı: " + javaDocComments);
            System.out.println("Diğer Yorum Satırı Sayısı: " + otherComments);
            System.out.println("Kod Satırı Sayısı: " + codeLines);
            System.out.println("LOC : " + loc);
            System.out.println("Fonksiyon Sayısı: " + functions);
            System.out.println("Yorum Sapma Yüzdesi: " + "% " + rounded);
        } catch (IOException e) {
            System.err.println("Java dosyası analiz edilirken bir hata oluştu:");
            e.printStackTrace();
        }
    }
   // Java dosyasındaki satırları inceleyerek sınıf adını bulmaya çalışır.
    private static String getClassName(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = CLASS_DESENİ.matcher(line);
            if (matcher.find()) {
            	// Sadece "class" anahtar kelimesi ile başlayan satırları dikkate al
                String classDeclaration = matcher.group();
                if (classDeclaration.startsWith("class")) {
                	// Eğer "class" anahtar kelimesi ile başlayan satır ise sınıf adını al
                    return matcher.group(1);
                }
            }
//            if (line.trim().startsWith("enum ")) {
//                return null;
//            }
        }
        return null;
    }
    
      //Javadoc Satırı Sayısını saymak için metod.
    private static long countjavaDocComments(List<String> lines) {
        long count = 0;
        boolean insideComment = false;

        // Her satır için döngü
        for (String line : lines) {
            String trimmedLine = line.trim(); // Satırın başındaki ve sonundaki boşlukları temizle

            // Eğer Javadoc yorum içerisinde değilsek ve satır "/*" ile başlıyorsa
            if (!insideComment && trimmedLine.startsWith("/**")) {
                insideComment = true; // Javadoc yorumu içindeyiz
            }

            
            if (insideComment && trimmedLine.endsWith("*/")) {
                insideComment = false; // Javadoc yorumu sona erdi
            }

            // Eğer Javadoc yorumu içindeysek ve satır ne başlangıç ne de sonlandırma işareti içeriyorsa ve boş değilse
            if (insideComment && !trimmedLine.startsWith("/**") && !trimmedLine.endsWith("*/") && !trimmedLine.isEmpty()) {
                count++; // Javadoc yorum satırı sayısını arttır
            }
        }

        return count; 
    }

    
    private static long countSingleLineComments(List<String> lines) {
        long count = 0;
        boolean insideMultiLineComment = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Çoklu satır yorumu içinde mi kontrol et
            if (!insideMultiLineComment && trimmedLine.startsWith("/*")) {
                insideMultiLineComment = true;
            }

            if (insideMultiLineComment && trimmedLine.endsWith("*/")) {
                insideMultiLineComment = false;
                continue; // Çoklu satır yorum içindeyken bu satırın tek satır yorum olmadığını belirt
            }

            // Tek satır yorumları sadece çoklu satır yorum içinde olmadığında ve
            // tek tırnak veya çift tırnak içinde değilken say
            if (!insideMultiLineComment && !isInsideQuotes(trimmedLine) && trimmedLine.contains("//")) {
                count++;
            }
        }

        return count;
    }
        //Bu metod, verilen bir satırın tek tırnak veya çift tırnak içinde olup olmadığını kontrol eder.
    private static boolean isInsideQuotes(String line) {
        boolean insideQuotes = false;
        boolean insideDoubleQuotes = false;
        boolean escape = false;

        // Satırı karakterlere ayır ve her karakter için kontrol et
        for (char c : line.toCharArray()) {
            // Tek tırnak içinde mi kontrol et
            if (c == '\'' && !escape) {
                insideQuotes = !insideQuotes; // Tek tırnak içindeysek durumu değiştir
            } else if (c == '"' && !escape) {
                insideDoubleQuotes = !insideDoubleQuotes; // Çift tırnak içindeysek durumu değiştir
            }

            // Kaçış karakteri kontrolü
            if (c == '\\' && !escape) {
                escape = true; // Kaçış karakteri varsa durumu true yap
            } else {
                escape = false; // Kaçış karakteri yoksa durumu false yap
            }

            // Tek tırnak veya çift tırnak içindeyken ve bu karakter `/` ise true döndür
            if ((insideQuotes || insideDoubleQuotes) && c == '/' && !escape) {
                return true;
            }
        }

        return false; // Tek tırnak veya çift tırnak içinde değilse false döndür
    }

    

    private static long countMultiLineComments(List<String> lines) {
        long count = 0;
        boolean insideMultiLineComment = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Javadoc yorumunu kontrol et
            if (trimmedLine.startsWith("/**") && !insideMultiLineComment) {
                continue; // Javadoc yorumlarını atla
            }

            // Çoklu satır yorumu içinde mi kontrol et
            if (!insideMultiLineComment && trimmedLine.startsWith("/*") && !trimmedLine.startsWith("/**") && !trimmedLine.isEmpty()) {
                insideMultiLineComment = true;
                count++;
            }

            if (insideMultiLineComment && trimmedLine.endsWith("*/")) {
                insideMultiLineComment = false;
            }
        }

        return count;
    }
    

    private static long countCodeLines(List<String> lines) {
        long count = 0;
        boolean insideMultiLineComment = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (!insideMultiLineComment && trimmedLine.startsWith("/*")) {
                insideMultiLineComment = true;
            }

            if (insideMultiLineComment && trimmedLine.endsWith("*/")) {
                insideMultiLineComment = false;
                continue;// Çoklu yorum satırı kapandığında diğer işlemleri atla
            }
           // Tekli yorum satırı veya çoklu yorum satırı içinde olmadığı durumlar
            if (!insideMultiLineComment && !trimmedLine.isEmpty() && !trimmedLine.startsWith("//")) {
                count++;
            }
        }

        return count;
    }

    private static long countFunctions(List<String> lines) {
        long count = 0;
        boolean insideFunction = false;
        for (String line : lines) {
            line = line.trim();
            if (!insideFunction && line.endsWith("{") && !line.contains(";")) {
                count++;
                insideFunction = true;
            } else if (insideFunction && line.endsWith("}")) {
                insideFunction = false;
            }
        }
        return count;
    }
}
