/*
 * Copyright (c) 2005-2006, Luke Plant
 * All rights reserved.
 * E-mail: <L.Plant.98@cantab.net>
 * Web: http://lukeplant.me.uk/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *
 *      * The name of Luke Plant may not be used to endorse or promote
 *        products derived from this software without specific prior
 *        written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Rewrite with Java language and modified for lorsource by Ildar Hizbulin 2011
 * E-mail: <hizel@vyborg.ru>
 */

package ru.org.linux.util.bbcode.nodes;

import ru.org.linux.util.bbcode.Parser;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/29/11
 * Time: 11:49 PM
*/
public class Node {
    Node parent=null;
    private final List<Node> children;
    String parameter;
    final Parser parser;

    public Node(Parser parser){
        this.parser = parser;
        children = new ArrayList<Node>();
    }

    public Node(Node parent, Parser parser){
        this.parser = parser;
        this.parent = parent;
        children = new ArrayList<Node>();
    }

    public Node getParent(){
        return parent;
    }

    public boolean allows(String tagname){
        assert false;
        return false;
    }

    public boolean prohibited(String tagname){
        return false;
    }

    public int lengthChildren(){
        return children.size();
    }

    public List<Node> getChildren(){
        return children;
    }

    public boolean isParameter(){
        return (parameter != null) && (parameter.length() > 0);
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String renderXHtml(Connection db){
        assert false;
        return "";
    }

    public String renderBBCode(){
        assert false;
        return "";
    }

    public String renderChildrenXHtml(Connection db){
        StringBuilder stringBuilder = new StringBuilder();
        for( Node child : children){
            stringBuilder.append(child.renderXHtml(db));
        }
        return stringBuilder.toString();
    }

    public String renderChildrenBBCode(){
        StringBuilder stringBuilder = new StringBuilder();
        for( Node child : children){
            stringBuilder.append(child.renderBBCode());
        }
        return stringBuilder.toString();

    }
}
