package ru.org.linux.util.bbcode.tags;

import ru.org.linux.util.bbcode.nodes.Node;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 7/5/11
 * Time: 11:55 AM
 */
public class CutTag extends HtmlEquivTag{

    private boolean renderCut;
    private String cutUrl;

    public CutTag(String name, Set<String> allowedChildren, String implicitTag){
        super(name, allowedChildren, implicitTag);
        renderCut = false;
        cutUrl = "";
    }

    public void setRenderOptions(boolean renderCut, String cutUrl) {
        this.renderCut = renderCut;
        this.cutUrl = cutUrl;
    }

    public String renderNodeXhtml(Node node){
        if(renderCut){
            return super.renderNodeXhtml(node);
        }else{
            return "<a href=\""+cutUrl+"\">Подробности</a>";
        }
    }
}
