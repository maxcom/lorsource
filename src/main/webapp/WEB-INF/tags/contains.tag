<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="collection" required="true" type="java.util.Collection" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%= collection.contains(value) %>
