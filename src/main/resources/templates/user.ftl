<#-- @ftlvariable name="pageUser" type="com.nazar.model.User" -->
<#-- @ftlvariable name="kweets" type="java.util.List<com.nazar.model.Kweet>" -->

<#import "template.ftl" as layout />

<@layout.mainLayout title="User ${pageUser.displayName}">
<h3>User's kweets</h3>

<@layout.kweets_list kweets=kweets></@layout.kweets_list>
</@layout.mainLayout>
