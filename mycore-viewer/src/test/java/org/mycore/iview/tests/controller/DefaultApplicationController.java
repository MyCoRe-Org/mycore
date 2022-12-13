/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
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

    private static Map<TestDerivate, String> derivateHTMLMapping;

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void init() {
        DefaultApplicationController.derivateHTMLMapping = new HashMap<>();
    }

    @Override
    public void setUpDerivate(WebDriver webdriver, TestDerivate testDerivate) {
        Path target = Paths.get(webpath);
        if(!Files.exists(target)){
            try(InputStream is = MCRSeleniumTestBase.class.getClassLoader().getResourceAsStream("testFiles.zip");
                ZipInputStream zis = new ZipInputStream(is)){
                extractZip(target.getParent().toAbsolutePath().toString(), zis);
            } catch (IOException e) {
                LOGGER.error("Could not unzip testFiles.zip", e);
            }

        }
        if (!derivateHTMLMapping.containsKey(testDerivate)) {
            try {
                String name = testDerivate.getName();

                if (testDerivate.getStartFile().endsWith(".pdf")) {
                    buildHTMLFile(name, testDerivate.getStartFile(), "MyCoRePDFViewer");
                } else {
                    buildHTMLFile(name, testDerivate.getStartFile(), "MyCoReImageViewer");
                }

                DefaultApplicationController.derivateHTMLMapping.put(testDerivate, buildFileName(name));
            } catch (IOException e) {
                LOGGER.error("Error while open connection to File Location!", e);
            }
        }
    }

    protected String buildHTMLFile(String name, String startFile, String page) throws IOException {
        try (InputStream viewerHTMLFileStream = DefaultApplicationController.class.getClassLoader()
            .getResourceAsStream("testStub/" + page + ".html")) {
            String content = IOUtils.toString(viewerHTMLFileStream, StandardCharsets.UTF_8);
            String result = content.replace("{$name}", name).replace("{$startFile}", startFile).replace("{$baseUrl}",
                MCRSeleniumTestBase.getBaseUrl(System.getProperty("BaseUrlPort")) + "/test-classes/testFiles/");
            String fileName = buildFileName(name);
            String resultLocation = webpath + "/" + fileName;
            File resultFile = new File(resultLocation);
            resultFile.getParentFile().mkdirs();
            try (FileOutputStream resultStream = new FileOutputStream(resultFile)) {
                IOUtils.write(result, resultStream, Charset.defaultCharset());
            }
            return resultLocation;
        }
    }

    private String buildFileName(String name) {
        return name + ".html";
    }

    @Override
    public void shutDownDerivate(WebDriver webdriver, TestDerivate testDerivate) {

    }

    @Override
    public void openViewer(WebDriver webdriver, TestDerivate testDerivate) {
        String path = null;
        path = MCRSeleniumTestBase.getBaseUrl(System.getProperty("BaseUrlPort")) + "/test-classes/testFiles/"
            + DefaultApplicationController.derivateHTMLMapping.get(testDerivate);
        LOGGER.info("Open Viewer with path : {}", path);
        webdriver.navigate().to(path);

        WebDriverWait wait = new WebDriverWait(webdriver, 5000);
        wait.until(ExpectedConditions
            .presenceOfAllElementsLocatedBy(By.xpath("/.//ol[contains(@class, 'chapterTreeDesktop')]")));

    }

    private void extractZip(String dest, ZipInputStream zipInputStream) throws IOException {
        ZipEntry nextEntry;
        zipInputStream.available();
        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String entryName = nextEntry.getName();
            String fileName = dest + "/" + entryName;
            File localFile = new File(fileName);

            if (nextEntry.isDirectory()) {
                localFile.mkdir();
            } else {
                localFile.createNewFile();
                try(FileOutputStream localFileOutputStream = new FileOutputStream(localFile)){
                    IOUtils.copyLarge(zipInputStream, localFileOutputStream, 0, nextEntry.getSize());
                }

            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
        LOGGER.info("File download complete!");
    }

}
