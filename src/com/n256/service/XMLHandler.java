/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.n256.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *
 * @author Nishan
 */

/*
This class uses JDOM library to handle hml file
*/
public class XMLHandler {
    String xmlFilePath = "";
    
    
    public XMLHandler(String xmlFilePath)
    {
        this.xmlFilePath = xmlFilePath;
    }
    
    //This method will return user availability
    public boolean isValidUser(String username)
    {
        username = username.toLowerCase();
        SAXBuilder builder = new SAXBuilder();
        
        //Make file object using available xml file
        File xmlFile = new File(xmlFilePath);
        if(!xmlFile.exists())
        {
            System.out.println("File not exists");
            return false;
        }
        try
        {
            //Make xml document
            Document document = (Document) builder.build(xmlFile);
            
            //Get root xml element to root node
            Element rootNode = document.getRootElement();
            
            //Get child elements of root element
            List list = rootNode.getChildren("user");

            //Iterate over each child element and check that element have username that we request. If avaialble, return true
            for(int i=0; i<list.size(); i++)
            {
                Element node = (Element) list.get(i);
                if(node.getChildText("username").trim().equals(username))
                {
                    return true;
                }
            }
            return false;
        }
        catch(JDOMException | IOException ex)
        {
            return false;
        }
    }
    
    public boolean isValidUserPassword(String username, String password)
    {
        username = username.toLowerCase();
        SAXBuilder builder = new SAXBuilder();
        //Make file object using available xml file given in xmlFilePath.
        File xmlFile = new File(xmlFilePath);
        if(!xmlFile.exists())
            return false;
        try
        {
            
            Document document = (Document) builder.build(xmlFile);
            Element rootNode = document.getRootElement();
            List<Element> list = rootNode.getChildren("user");

            for(int i=0; i<list.size(); i++)
            {
                Element node = list.get(i);
                if(node.getChildText("username").trim().equals(username))
                {
                    if(node.getChildText("password").equals(password))
                        return true;
                }
            }
            return false;
        }
        catch(JDOMException | IOException ex)
        {
            return false;
        }
    }
    
    public void addNewUser(String username, String password)
    {
        username = username.toLowerCase();
        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(xmlFilePath);
        Document document = null;
        try
        {
            if(xmlFile.exists())
            {
                document = (Document) builder.build(xmlFile);
            }
            else{
                Element users = new Element("users");
                document = new Document(users);
                document.setRootElement(users);
            }
            Element user = new Element("user");
            user.addContent(new Element("username").setText(username));
            user.addContent(new Element("password").setText(password));
            
            document.getRootElement().addContent(user);
            
            XMLOutputter xmlOutput = new XMLOutputter();
            
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(document, new FileWriter(xmlFilePath, false));
        }
        catch (IOException | JDOMException ex) {
            Logger.getLogger(XMLHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
