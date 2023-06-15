import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class HumanBenchmarkAutomation {
  public static WebDriver driver;
  public static void main(String[] args) {
    System.setProperty("webdriver.chrome.driver", "./drivers/chromedriver.exe");
    driver = new ChromeDriver();
    try {
      driver.manage().deleteAllCookies();
      driver.manage().window().maximize();
      driver.get("https://humanbenchmark.com");
      BenchmarkTest[] tests = {
        new ReactionTime(),
        new SequenceMemory(),
        new AimTrainer(),
        new NumberMemory(1),
        new VerbalMemory(1),
        new Chimp(1),
        new VisualMemory(1),
        new Typing(),
      };
      for (var t : tests) performTest(t);
      Thread.sleep(1000);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      driver.close();
    }
  }

  public interface BenchmarkTest {
    String getXpath();
    void perform();
  }

  public static void performTest(BenchmarkTest b) throws InterruptedException {
    var e = driver.findElement(By.xpath(b.getXpath()));
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: \"center\", inline: \"center\"})", e);
    e.click();
    b.perform();
    Thread.sleep(2000);
    driver.navigate().back();
  }

  public static class SequenceMemory implements BenchmarkTest {
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/sequence\"]";
    }

    @Override
    public void perform() {
      var button = By.xpath("//*[@data-test=\"true\"]//button");
      driver.findElement(button).click();
      var wait = new FluentWait<>(driver)
        .withTimeout(Duration.ofSeconds(10))
        .pollingEvery(Duration.ofMillis(150))
        .ignoring(Exception.class);
      var activeSquare = By.xpath("//*[@data-test=\"true\"]//*[@class=\"square active\"]");
      for (long i = 1;; i++) {
        List<WebElement> squares = new ArrayList<>();
        for (int j = 0; j < i; j++) {
          wait.until(ExpectedConditions.presenceOfElementLocated(activeSquare));
          squares.add(driver.findElement(activeSquare));
        }
        for (var s : squares) new Actions(driver).moveToElement(s).click().perform();
      }
    }
  }

  public static class VisualMemory implements BenchmarkTest {
    private final long max;
    VisualMemory() {
      max = Long.MAX_VALUE;
    }
    VisualMemory(long max_level) {
      max = max_level;
    }
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/memory\"]";
    }

    @Override
    public void perform() {
      long lives = 3;
      var button = By.xpath("//*[@data-test=\"true\"]//button");
      driver.findElement(button).click();
      var active = By.xpath("//*[@data-test=\"true\"]//*[contains(@class, \"active\")]");
      var notActive = By.xpath("//*[@data-test=\"true\"]//*[contains(@class, \"eut2yre1\")][not(contains(@class, \"active\"))]");
      var wait = new FluentWait<>(driver)
        .withTimeout(Duration.ofSeconds(10))
        .pollingEvery(Duration.ofMillis(100))
        .ignoring(Exception.class);
      try {
        for (long i = 1;; i++) {
          wait.until(ExpectedConditions.presenceOfElementLocated(active));
          var squares = driver.findElements(i > max ? notActive : active);
          wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(active)));
          if (i > max) {
            for (int j = 0; j < 3; j++) new Actions(driver).moveToElement(squares.get(j)).click().perform();
            lives--;
            if (lives <= 0) break;
          } else for (var s : squares) new Actions(driver).moveToElement(s).click().perform();
          wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(active)));
        }
      } catch (Exception ignored) {}
    }
  }

  public static class Chimp implements BenchmarkTest {
    private final long max;
    Chimp() {
      max = 40;
    }
    Chimp(long max_level) {
      max = Math.min(40L, max_level);
    }
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/chimp\"]";
    }

    @Override
    public void perform() {
      long lives = 3;
      var button = By.xpath("//*[@data-test=\"true\"]//button");
      for (long i = 4; i <= 40; i++) {
        driver.findElement(button).click();
        var numbers = driver.findElements(By.xpath("//*[@data-test=\"true\"]//*[@class=\"desktop-only\"]//*[@data-cellnumber]/*"));
        numbers.sort(Comparator.comparingLong(e -> Long.parseLong(e.getText())));
        if (i > max) {
          new Actions(driver).moveToElement(numbers.get(1)).click().perform();
          lives--;
          if (lives <= 0) break;
          continue;
        }
        for (var n : numbers) new Actions(driver).moveToElement(n).click().perform();
      }
    }
  }

  public static class VerbalMemory implements BenchmarkTest {
    private final long max;
    VerbalMemory() {
      max = Long.MAX_VALUE;
    }
    VerbalMemory(long max_rounds) {
      max = max_rounds;
    }
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/verbal-memory\"]";
    }

    @Override
    public void perform() {
      var button = By.xpath("//*[@data-test=\"true\"]//button");
      var b = driver.findElement(button);
      new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> b.getText().equals("Start"));
      b.click();
      var buttons = driver.findElements(button);
      Set<String> mem = new HashSet<>();
      try {
        for (long i = 0;; i++) {
          String word = driver.findElement(By.xpath("//*[@data-test=\"true\"]//*[@class=\"word\"]")).getText();
          buttons.get((i < max) == mem.contains(word) ? 0 : 1).click();;
          mem.add(word);
        }
      } catch (Exception ignored) {}
    }
  }

  public static class NumberMemory implements BenchmarkTest {
    private final long max;
    NumberMemory() {
      max = Long.MAX_VALUE;
    }
    NumberMemory(long max_digits) {
      max = max_digits;
    }
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/number-memory\"]";
    }

    @Override
    public void perform() {
      var button = By.xpath("//*[@data-test=\"true\"]//button");
      var input = By.xpath("//*[@data-test=\"true\"]//input");
      driver.findElement(button).click();
      for (long i = 1;; i++) {
        String number = driver.findElement(By.className("big-number")).getText();
        new WebDriverWait(driver, Duration.ofSeconds(5L*i)).until(ExpectedConditions.presenceOfElementLocated(input));
        driver.findElement(input).sendKeys(i <= max ? number : "0");
        driver.findElement(button).click();
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(button));
        var b = driver.findElement(button);
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> b.getText().length() > 0);
        if (!b.getText().equals("NEXT")) break;
        b.click();
      }
    }
  }

  public static class AimTrainer implements BenchmarkTest {
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/aim\"]";
    }

    @Override
    public void perform() {
      var target = By.xpath("//*[@data-aim-target=\"true\"]");
      var actions = new Actions(driver);
      actions.moveToElement(driver.findElement(By.xpath("//*[@data-test=\"true\"]"))).click().perform();
      for (int i = 0; i < 30; i++)
        actions.moveToElement(driver.findElement(target)).click().perform();
    }
  }

  public static class Typing implements BenchmarkTest {
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/typing\"]";
    }

    @Override
    public void perform() {
      var letters = driver.findElement(By.className("letters")).findElements(By.xpath("./*"));
      StringBuilder text = new StringBuilder();
      for (var l : letters) text.append(l.getAttribute("innerHTML"));
      new Actions(driver).sendKeys(text).perform();
    }
  }

  public static class ReactionTime implements BenchmarkTest {
    @Override
    public String getXpath() {
      return "//*[@href=\"/tests/reactiontime\"]";
    }

    @Override
    public void perform() {
      quickReact();
    }

    public void quickReact() {
      String script = """
        let banner = document.getElementsByClassName('view-splash')[0];
        let observer = new MutationObserver(s => {
          if (s[0].target.classList.contains('view-go'))
            banner.children[0].dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));
        });
        observer.observe(banner, {attributes: true});
       """;
      ((JavascriptExecutor) driver).executeScript(script);
      var banner = driver.findElement(By.className("view-splash"));
      var clickAction = new Actions(driver).moveToElement(banner).click().build();
      clickAction.perform();
      var wait = new FluentWait<>(driver)
        .withTimeout(Duration.ofSeconds(20))
        .pollingEvery(Duration.ofMillis(2000))
        .ignoring(Exception.class);
      var isResult = ExpectedConditions.presenceOfElementLocated(By.className("view-result"));
      for (int i = 0; i < 4; i++) {
        wait.until(isResult);
        clickAction.perform();
      }
      wait.until(ExpectedConditions.presenceOfElementLocated(By.className("view-score")));
    }

    public void slowReact() {
      var banner = driver.findElement(By.className("view-splash"));
      var clickAction = new Actions(driver).moveToElement(banner).click().build();
      clickAction.perform();
      var wait = new FluentWait<>(driver)
        .withTimeout(Duration.ofSeconds(10))
        .pollingEvery(Duration.ofMillis(1))
        .ignoring(Exception.class);
      var isGreen = ExpectedConditions.presenceOfElementLocated(By.className("view-go"));
      for (int i = 0; i < 5; i++) {
        wait.until(isGreen);
        clickAction.perform();
        clickAction.perform();
      }
    }
  }

}
