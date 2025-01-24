package com.project.howmuch_be.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RestaurantSearchService {

    @Value("${webdriver.chrome.dns.servers}")
    private String dnsServers;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void setUp() {
        // Set ChromeDriver path from environment variable
        String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromeDriverPath != null && !chromeDriverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        }
        
        // Set Chrome binary path from environment variable
        String chromePath = System.getenv("CHROME_PATH");
        if (chromePath != null && !chromePath.isEmpty()) {
            System.setProperty("webdriver.chrome.binary", chromePath);
        }
    }

    private WebDriver createWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--remote-allow-origins=*",
            "--disable-blink-features=AutomationControlled"
        );
        
        // 프록시 설정 제거
        options.addArguments("--proxy-server='direct://'");
        options.addArguments("--proxy-bypass-list=*");
        
        // 랜덤 User-Agent 설정
        options.addArguments(String.format(
            "user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36",
            120 + (int)(Math.random() * 10)
        ));
        
        // 성능 최적화 설정
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.managed_default_content_settings.images", 1);
        prefs.put("profile.default_content_setting_values.cookies", 1);
        options.setExperimentalOption("prefs", prefs);
        
        return new ChromeDriver(options);
    }

    public List<Map<String, Object>> searchRestaurants(String searchTerm) {
        WebDriver driver = null;
        List<Map<String, Object>> results = new ArrayList<>();
        long timeLimit = 60000; // 1분
        long startTime = System.currentTimeMillis();
        String normalizedSearchTerm = searchTerm.replaceAll("\\s+", "");
        
        try {
            log.info("1. 검색 시작: {}", searchTerm);
            
            driver = createWebDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // 네이버 지도로 이동
            driver.get("https://map.naver.com/p");
            log.info("2. 네이버 지도 페이지 로드 완료");
            
            // 검색창 찾기
            WebElement searchBox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search"))
            );
            log.info("3. 검색창 찾기 성공");
            
            // 지역명을 포함한 검색어 입력
            String searchQuery = "포항시 " + searchTerm;
            searchBox.sendKeys(searchQuery);
            searchBox.sendKeys(Keys.ENTER);
            log.info("4. 검색어 입력 및 검색 실행: {}", searchQuery);
            
            // searchIframe 찾기
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchIframe")));
            log.info("5. searchIframe 찾기 성공");
            
            // iframe 전환
            driver.switchTo().frame("searchIframe");
            log.info("6. searchIframe으로 전환 성공");

            // 검색 결과 스크롤
            log.info("Scrolling for more results...");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            WebElement container = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("#_pcmap_list_scroll_container"))
            );
            
            // 여러 번 스크롤하여 더 많은 결과 로드
            for (int i = 0; i < 10; i++) {
                // 120초 제한 체크
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    log.info("Total search time limit (60s) reached. Returning {} results.", results.size());
                    return results;
                }
                
                js.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight", container);
                Thread.sleep(1000);
                
                List<WebElement> currentResults = driver.findElements(By.cssSelector("li.UEzoS"));
                log.info("Found {} results after scroll {}", currentResults.size(), i + 1);
                
                for (WebElement restaurant : currentResults) {
                    try {
                        Map<String, Object> restaurantInfo = new HashMap<>();
                        
                        // 각 식당 처리 시작 시간 기록
                        long restaurantStartTime = System.currentTimeMillis();
                        
                        // 식당 이름 가져오기
                        WebElement nameElement = restaurant.findElement(By.cssSelector(".place_bluelink .TYaxT"));
                        String name = nameElement.getText();
                        restaurantInfo.put("식당이름", name);
                        log.info("Processing restaurant: {}", name);
                        
                        // 식당 클릭하여 상세 정보 보기
                        WebElement clickElement = restaurant.findElement(By.cssSelector(".place_bluelink"));
                        wait.until(ExpectedConditions.elementToBeClickable(clickElement));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", clickElement);
                        log.info("8. 첫 번째 식당 클릭 성공");
                        
                        driver.switchTo().defaultContent();
                        log.info("9. 기본 프레임으로 전환 성공");
                        
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));
                        log.info("10. entryIframe 찾기 성공");
                        
                        driver.switchTo().frame("entryIframe");
                        log.info("11. entryIframe으로 전환 성공");
                        
                        // 상세 정보 수집
                        collectRestaurantDetails(driver, wait, restaurantInfo, searchTerm);
                        
                        // 디버그를 위한 로깅 추가
                        log.info("식당 정보: {}", restaurantInfo);
                        log.info("메뉴 개수: {}", ((List<?>)restaurantInfo.get("메뉴")).size());
                        
                        // 유효한 메뉴가 있는 경우만 추가
                        List<Map<String, Object>> menus = (List<Map<String, Object>>) restaurantInfo.get("메뉴");
                        log.info("원본 메뉴: {}", menus);
                        
                        // 검색어를 포함하는 메뉴만 필터링
                        List<Map<String, Object>> filteredMenus = menus.stream()
                            .filter(menu -> {
                                String menuName = ((String) menu.get("메뉴명")).replaceAll("\\s+", "");
                                return menuName.contains(normalizedSearchTerm);
                            })
                            .collect(Collectors.toList());
                        
                        log.info("필터링된 메뉴: {}", filteredMenus);
                        
                        // 리뷰 필터링 (4글자 이상)
                        List<String> reviews = (List<String>) restaurantInfo.get("리뷰");
                        List<String> validReviews = reviews.stream()
                            .filter(review -> review.length() >= 4)
                            .collect(Collectors.toList());
                        
                        restaurantInfo.put("메뉴", filteredMenus);
                        restaurantInfo.put("리뷰", validReviews);
                        
                        // 메뉴나 리뷰가 있는 경우 추가
                        if (!filteredMenus.isEmpty() || !validReviews.isEmpty()) {
                            results.add(restaurantInfo);
                            // 결과 추가 후에도 시간 체크
                            if (System.currentTimeMillis() - startTime > timeLimit) {
                                log.info("Total search time limit (60s) reached after adding result. Returning {} results.", results.size());
                                return results;
                            }
                        }
                        
                        // 검색 결과 목록으로 돌아가기
                        driver.switchTo().defaultContent();
                        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("searchIframe"));
                        
                        // 각 식당별 30초 제한 체크
                        if (System.currentTimeMillis() - restaurantStartTime > 30000) {
                            log.info("Restaurant processing time limit (30s) reached. Skipping to next.");
                            continue;
                        }
                        
                    } catch (Exception e) {
                        log.error("Error processing restaurant: {}", e.getMessage());
                        continue;
                    }
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("Error during search process: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private void collectRestaurantDetails(WebDriver driver, WebDriverWait wait, Map<String, Object> restaurantInfo, String searchTerm) {
        try {
            // 검색어 저장
            restaurantInfo.put("searchTerm", searchTerm);
            
            WebElement scriptElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//script[contains(text(), '__APOLLO_STATE__')]")
            ));
            
            String scriptContent = scriptElement.getAttribute("innerHTML");
            Map<String, Object> data = parseApolloState(scriptContent);
            
            extractLocation(data, restaurantInfo);
            extractMenus(data, restaurantInfo, driver);
            extractReviews(driver, wait, restaurantInfo);
            
            // 리뷰 수 추출
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> value = (Map<String, Object>) entry.getValue();
                    if (value.containsKey("visitorReviewsTotal")) {
                        restaurantInfo.put("reviewCount", value.get("visitorReviewsTotal"));
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            setDefaultValues(restaurantInfo);
        }
    }

    private Map<String, Object> parseApolloState(String scriptContent) throws Exception {
        Pattern pattern = Pattern.compile("window\\.__APOLLO_STATE__\\s*=\\s*(\\{.*\\});", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(scriptContent);
        
        if (matcher.find()) {
            return objectMapper.readValue(matcher.group(1), Map.class);
        }
        throw new Exception("APOLLO_STATE not found in script content");
    }

    private void extractLocation(Map<String, Object> data, Map<String, Object> restaurantInfo) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> value = (Map<String, Object>) entry.getValue();
                if (value.containsKey("coordinate")) {
                    Map<String, Object> coordinate = (Map<String, Object>) value.get("coordinate");
                    restaurantInfo.put("위치", new HashMap<String, Object>() {{
                        put("x", coordinate.get("x"));
                        put("y", coordinate.get("y"));
                    }});
                    break;
                }
            }
        }
    }

    private void extractMenus(Map<String, Object> data, Map<String, Object> restaurantInfo, WebDriver driver) {
        List<Map<String, Object>> menus = new ArrayList<>();
        String searchTerm = (String) restaurantInfo.get("searchTerm");
        String normalizedSearchTerm = searchTerm.replaceAll("\\s+", "");
        
        log.info("Extracting menus from data keys: {}", data.keySet());

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey().startsWith("Menu:") || 
                (entry.getKey().contains("menu") && entry.getValue() instanceof Map)) {
                Map<String, Object> value = (Map<String, Object>) entry.getValue();
                log.info("Found menu entry: {}", value);

                // 메뉴 정보 추출
                String name = (String) value.getOrDefault("name", "");
                String price = (String) value.getOrDefault("price", "");
                
                // 유효한 메뉴만 추가
                if (!name.isEmpty() && price != null && !price.isEmpty()) {
                    menus.add(new HashMap<String, Object>() {{
                        put("메뉴명", name);
                        put("가격", price);
                    }});
                }
            }
        }

        // 메뉴를 찾지 못한 경우 HTML에서 직접 찾기
        if (menus.isEmpty()) {
            try {
                WebElement menuSection = driver.findElement(By.cssSelector(".place_section_content"));
                List<WebElement> menuItems = menuSection.findElements(By.cssSelector(".K0PDV"));
                
                for (WebElement menuItem : menuItems) {
                    String name = menuItem.findElement(By.cssSelector(".name")).getText();
                    String price = menuItem.findElement(By.cssSelector(".price")).getText();
                    
                    if (!name.isEmpty() && price != null && !price.isEmpty()) {
                        menus.add(new HashMap<String, Object>() {{
                            put("메뉴명", name);
                            put("가격", price);
                        }});
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to extract menus from HTML: {}", e.getMessage());
            }
        }

        log.info("Extracted {} menus", menus.size());
        // 필터링된 메뉴만 저장
        restaurantInfo.put("메뉴", menus);
        
        // 검색어를 포함하는 메뉴만 필터링
        List<Map<String, Object>> filteredMenus = menus.stream()
            .filter(menu -> {
                String menuName = ((String) menu.get("메뉴명")).replaceAll("\\s+", "");
                return menuName.contains(normalizedSearchTerm);
            })
            .collect(Collectors.toList());
        
        // 필터링된 메뉴만 저장
        restaurantInfo.put("메뉴", filteredMenus);
        
        // 메뉴가 있는 경우에만 식당 정보 추가
        if (!filteredMenus.isEmpty()) {
            restaurantInfo.put("hasMatchingMenu", true);
        }
    }

    private void extractReviews(WebDriver driver, WebDriverWait wait, Map<String, Object> restaurantInfo) {
        try {
            // 리뷰 탭이 있는지 확인
            List<WebElement> reviewTabs = driver.findElements(
                By.xpath("//span[contains(@class, 'veBoZ') and text()='리뷰']")
            );
            
            if (reviewTabs.isEmpty()) {
                log.info("리뷰 탭이 없습니다.");
                restaurantInfo.put("리뷰", new ArrayList<>());
                return;
            }

            // 리뷰 버튼 클릭
            WebElement reviewButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[contains(@class, 'veBoZ') and text()='리뷰']")
            ));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reviewButton);
            Thread.sleep(2000);  // 리뷰 로딩을 위해 대기 시간 증가

            // 리뷰 목록 가져오기
            List<WebElement> reviewElements;
            try {
                reviewElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("div.YeINN, div.ZZ4OK, div.pui__vn15t2 a")
                ));
            } catch (Exception e) {
                log.info("리뷰가 없거나 로딩에 실패했습니다.");
                restaurantInfo.put("리뷰", new ArrayList<>());
                return;
            }

            List<String> reviews = new ArrayList<>();
            
            for (int i = 0; i < Math.min(reviewElements.size(), 10); i++) {
                try {
                    String reviewText = reviewElements.get(i).getText().trim();
                    // "더보기" 버튼과 빈 텍스트 필터링
                    if (!reviewText.isEmpty() && !reviewText.equals("더보기") && reviewText.length() >= 4) {
                        reviews.add(reviewText);
                    }
                } catch (Exception e) {
                    log.debug("리뷰 텍스트 추출 중 오류: {}", e.getMessage());
                    continue;
                }
            }
            
            log.info("수집된 리뷰 수: {}", reviews.size());
            restaurantInfo.put("리뷰", reviews);
            
        } catch (Exception e) {
            log.error("리뷰 수집 중 오류 발생: {} (단계: {})", e.getMessage(), getCurrentStep());
            restaurantInfo.put("리뷰", new ArrayList<>());
        }
    }

    private void setDefaultValues(Map<String, Object> restaurantInfo) {
        Map<String, String> emptyLocation = new HashMap<>();
        emptyLocation.put("x", "");
        emptyLocation.put("y", "");
        
        restaurantInfo.putIfAbsent("위치", emptyLocation);
        restaurantInfo.putIfAbsent("메뉴", new ArrayList<>());
        restaurantInfo.putIfAbsent("name", "");
        restaurantInfo.putIfAbsent("reviewCount", 0);
        restaurantInfo.putIfAbsent("리뷰", new ArrayList<>());
    }

    private Map<String, Object> orderAndFilterResults(Map<String, Object> restaurantInfo, String searchTerm) {
        Map<String, Object> orderedInfo = new LinkedHashMap<>();
        orderedInfo.put("id", null);
        String name = (String) restaurantInfo.get("식당이름");
        orderedInfo.put("name", name != null ? name : "");
        
        // 메뉴 정보 변환
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> menus = ((List<Map<String, Object>>) restaurantInfo.getOrDefault("메뉴", new ArrayList<>()))
            .stream()
            .map(menu -> {
                Map<String, Object> convertedMenu = new HashMap<>();
                convertedMenu.put("name", menu.get("메뉴명"));
                convertedMenu.put("price", Integer.parseInt(String.valueOf(menu.get("가격"))));
                return convertedMenu;
            })
            .collect(Collectors.toList());
        orderedInfo.put("menus", menus);
        
        orderedInfo.put("location", restaurantInfo.getOrDefault("위치", new HashMap<String, Object>() {{
            put("x", "");
            put("y", "");
        }}));
        
        // reviewCount 처리 개선
        int reviewCount = 0;
        try {
            reviewCount = Integer.parseInt(String.valueOf(restaurantInfo.get("reviewCount")));
            log.debug("Restaurant: {}, reviewCount: {}", name, reviewCount);
            log.debug("Setting reviewCount: {}", reviewCount);
        } catch (Exception e) {
            log.debug("Failed to parse reviewCount for {}", name);
            reviewCount = 0;
        }
        
        // 리뷰수와 reviewCount 모두 같은 값을 사용
        orderedInfo.put("리뷰수", reviewCount);
        orderedInfo.put("reviewCount", reviewCount);
        orderedInfo.put("reviews", restaurantInfo.getOrDefault("리뷰", new ArrayList<>()));

        String searchTermLower = searchTerm.replaceAll("\\s+", "").toLowerCase();

        // 메뉴 필터링
        List<Map<String, Object>> filteredMenus = new ArrayList<>();
        boolean hasMatchingMenu = false;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> currentMenus = (List<Map<String, Object>>) orderedInfo.get("menus");
        for (Map<String, Object> menu : currentMenus) {
            String menuName = ((String) menu.get("name")).replaceAll("\\s+", "").toLowerCase();
            if (menuName.contains(searchTermLower)) {
                hasMatchingMenu = true;
                filteredMenus.add(menu);
            }
        }

        if (hasMatchingMenu) {
            orderedInfo.put("menus", filteredMenus);
            log.info("Found restaurant with name: {} and reviewCount: {}", name, reviewCount);
            return orderedInfo;
        }

        return null;
    }

    private void switchToFrame(WebDriver driver, String frameId, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id(frameId)));
            log.debug("iframe 전환 성공: {}", frameId);
        } catch (Exception e) {
            log.warn("iframe 전환 실패: {}", frameId);
            driver.switchTo().defaultContent();
        }
    }

    private String getCurrentStep() {
        // 현재 진행 중인 단계를 반환
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            return stackTrace[3].getMethodName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
