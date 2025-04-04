/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.iview.tests.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.selenium.MCRSeleniumTestBase;
import org.mycore.iview.tests.model.TestDerivate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DefaultApplicationController extends ApplicationController {

    private static final String webpath = "target/test-classes/testFiles";
    private static final Logger LOGGER = LogManager.getLogger();
    private static Map<TestDerivate, String> derivateHTMLMapping;

    @Override
    public void init() {
        derivateHTMLMapping = new HashMap<>();
    }

    @Override
    public void setUpDerivate(WebDriver webdriver, String baseURL, TestDerivate testDerivate) {
        Path target = Paths.get(webpath);
        if (!Files.exists(target)) {
            try (InputStream is = MCRSeleniumTestBase.class.getClassLoader().getResourceAsStream("testFiles.zip");
                ZipInputStream zis = new ZipInputStream(is)) {
                extractZip(target.getParent(), zis);
            } catch (IOException e) {
                LOGGER.error("Could not unzip testFiles.zip", e);
            }

        }
        try {
            String name = testDerivate.getName();

            if (testDerivate.getStartFile().endsWith(".pdf")) {
                buildHTMLFile(baseURL, name, testDerivate.getStartFile(), "MyCoRePDFViewer");
            } else {
                buildHTMLFile(baseURL, name, testDerivate.getStartFile(), "MyCoReImageViewer");
            }

            derivateHTMLMapping.put(testDerivate, buildFileName(name));
        } catch (IOException e) {
            LOGGER.error("Error while open connection to File Location!", e);
        }
    }

    protected String buildHTMLFile(String baseUrl, String name, String startFile, String page) throws IOException {
        try (InputStream viewerHTMLFileStream = DefaultApplicationController.class.getClassLoader()
            .getResourceAsStream("testStub/" + page + ".html")) {
            String content = new String(viewerHTMLFileStream.readAllBytes(), StandardCharsets.UTF_8);
            String result = content.replace("{$name}", name).replace("{$startFile}", startFile).replace("{$baseUrl}",
                baseUrl + "/test-classes/testFiles/");
            String fileName = buildFileName(name);
            String resultLocation = webpath + "/" + fileName;
            Path resultFile = Paths.get(resultLocation);
            Files.createDirectories(resultFile.getParent());
            Files.writeString(resultFile, result, Charset.defaultCharset());

            return resultLocation;
        }
    }

    private String buildFileName(String name) {
        return name + ".html";
    }

    @Override
    public void shutDownDerivate(WebDriver webdriver, TestDerivate testDerivate) {
        //TODO: Is only implementation, with only one usage. What is the purpose of this method?
    }

    @Override
    public void openViewer(WebDriver webdriver, String baseURL, TestDerivate testDerivate) {
        String path = null;
        path = baseURL + "/test-classes/testFiles/"
            + derivateHTMLMapping.get(testDerivate);
        LOGGER.info("Open Viewer with path : {}", path);
        webdriver.navigate().to(path);

        WebDriverWait wait = new WebDriverWait(webdriver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions
            .presenceOfAllElementsLocatedBy(By.xpath("/.//ol[contains(@class, 'chapterTreeDesktop')]")));

    }

    private void extractZip(Path dest, ZipInputStream zipInputStream) throws IOException {
        ZipEntry nextEntry;
        zipInputStream.available();
        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String entryName = nextEntry.getName();
            Path localFile = dest.resolve(entryName);
            if (nextEntry.isDirectory()) {
                Files.createDirectories(localFile);
            } else {
                Files.copy(zipInputStream, localFile, StandardCopyOption.REPLACE_EXISTING);
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
        LOGGER.info("File download complete!");
    }

}
