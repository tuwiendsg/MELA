/* 
* JS functions for Flexiant theme
* written by K Munro October 2013
*/
jQuery(document).ready(function(a){a("#wpmem_login").addClass("col-md-5 col-sm-5");a("#wpmem_reg").addClass("col-md-6 col-sm-6 col-md-offset-1 col-sm-offset-1");a(".sub-menu").addClass("sub-nav-dropdown");a(".first").pageslide({direction:"left",modal:!0});a("a.modal-close").click(function(b){b.preventDefault();a.pageslide.close()});a(".second").pageslide({direction:"left",modal:!0});a("#myTab li:eq(0)").addClass("active");a(".tab-content div:eq(0)").addClass("active in");a(".blue-cta-bar .tab-content div:eq(0)").addClass("active in");
var d=document.location.hash;d&&a("#myTab a[href="+d.replace("tab_","")+"]").tab("show");a("#myTab a").on("shown",function(a){window.location.hash=a.target.hash.replace("#","#tab_")});a("#archives-3 select").addClass("form-control");a("#taxonomy-2 select").addClass("form-control");a("#carousel2").elastislide({imageW:340,minItems:5});a.browser.msie&&a(".video-pad a").addClass("active");navigator.userAgent.match(/(iPod|iPhone|iPad)/)&&a(".video-pad a").addClass("active");a(".blog-footer div").removeClass("btn btn-blue");
a("p.form-submit #submit").addClass("btn btn-orange");(function(a){a.fn.removeStyle=function(e){var c=RegExp(e+"[^;]+;?","g");return this.each(function(){a(this).attr("style",function(a,b){return b.replace(c,"")})})}})(jQuery);a(window).on("resize",function(){function b(b){var c=0;b.each(function(){var b=a(this).height();b>c&&(c=b)});b.height(c)}b(a("ul.features-list li a"));b(a("p.video-desc"));a(window).width()}).trigger("resize");a(".hme-client-logos").width(3500)});




jQuery(".fco").click(function() {
    jQuery('html, body').animate({
        scrollTop: jQuery("#fco-plugins").position().top
    }, 2000);
});


jQuery(".other").click(function() {
    jQuery('html, body').animate({
        scrollTop: jQuery("#other-plugins").position().top
    }, 2000);
});
jQuery(".image-table").click(function() {
    jQuery('html, body').animate({
        scrollTop: jQuery("#images-table").position().top
    }, 2000);
});
jQuery(".utilities").click(function() {
    jQuery('html, body').animate({
        scrollTop: jQuery("#utilities").position().top
    }, 2000);
});







   jQuery("a[href='#top']").click(function() {
  jQuery("html, body").animate({ scrollTop: 0 }, "slow");
  return false;
});






   
   /*
   
   jQuery('.question-mark').hover(function() {
    jQuery('.mydiv').addClass('visibility');
   })
   
   jQuery('.invisible_div').mouseout(function() {
        jQuery('.mydiv').removeClass('visibility')
   })
   
     jQuery('body').click(function() {
        jQuery('.mydiv').removeClass('visibility')
   })
     
   */
     
     jQuery('img').tooltip({
                           
                           delay: { show: 100, hide: 1000 },
                           animation: true
     });
    
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     