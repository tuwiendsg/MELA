window.hbspt = window.hbspt || {};
window.hbspt.cta = window.hbspt.cta || {

    __hstc: "",
    __hssc: "",
    __utk: "",
    email: "",
    contentPageId: "",

    cookieListenerAdded: false,
    canonicalURL: "",

    debug: (window.location.href.toLowerCase().indexOf("hsctadebug") >= 0),

    log: function(mesg) {
        if (this.debug && window.console) {
           window.console.log(new Date()  + " CTA: " + mesg);
        }
    },

    load: function(portalId, placement) {

        var self = this;

        self.log(placement + " loading");

        try {
            self.log(placement + " trying to hide default cta");
            document.getElementById("hs-cta-" + placement).style.visibility="hidden";
        } catch(err) {
            self.log(placement + " couldn't be hidden");
        }

        this.__utk  = this.__utk  || this.getCookieValue('hubspotutk');
        this.__hssc = this.__hssc || this.getCookieValue('__hssc');
        this.__hstc = this.__hstc || this.getCookieValue('__hstc');
        this.canonicalURL = this.canonicalURL || this.getCanonicalURL();

        if ((window.location.host.indexOf("hubspot") > 0 || window.location.host.indexOf("hs-sites") > 0) && this.getParameterByName("_preview"))
        {
            this.email = this.getParameterByName("email");
        }

        if(!this.contentPageId) {
                window._hsq = window._hsq || [];
                window._hsq.push(['addContentMetadataListener', self.setContentPageId])
            }

        if (this.__utk === "" || this.__hssc === "" || this.__hstc === "") {

            self.log(placement + " analytics data needed");

            if (!this.cookieListenerAdded) {
                this.cookieListenerAdded = true;

                window._hsq = window._hsq || [];

                if (this.__utk === "") {
                    self.log(placement + " requesting utk");
                    window._hsq.push(['addUserTokenListener', function(utk) {
                        self.__utk = utk;
                        self.log(placement + " got utk '" + utk + "'");
                    }]);
                }

                self.log(placement + " requesting hs*");
                // this is tricksy: _hsq is an array initially, but becomes a function that returns immediately after analytics loads
                window._hsq.push(['addCookieListener', function(__hstc, __hssc) {
                    self.__hstc = __hstc;
                    self.__hssc = __hssc;
                    self.log(placement + " got hstc, hssc '" + __hstc + "','"  + __hssc + "'");
                    // Jess will send utk here soon too
                }]);

            }

            self.log(placement + " waiting for analytics");
            // give analytics a little time to load
            window.setTimeout(function() {
                self.displayCTA(portalId, placement);
                }, 1000
            );
        }
        else {
            self.displayCTA(portalId, placement);
        }
    },

    getCanonicalURL: function() {
        var links = document.getElementsByTagName("link");
        for (var i = 0; i < links.length; i++)
        {
            var link = links[i];
            if (link.rel === "canonical")
            {
                this.log("got canonical url " + link.href);
                return link.href;
            }
        }

        // we tried.
        this.log("returning location as canonical url " + window.location.href);
        return window.location.href;
    },

    getParameterByName: function(name) {
        name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
        var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(window.location.search);
        return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    },

    displayCTA: function(portalId, placement) {
        var hsjs = document.createElement("script"),
            self = this;

        self.log(placement + " displaying cta with utk, hssc, hstc '" + this.__utk + "', '" + this.__hssc + "', '" + this.__hstc + "'");

        hsjs.type = "text/javascript";
        hsjs.async = true;
        hsjs.src = "//cta-service-cms2.hubspot.com/cs/loader-v2.js?pg="+ placement + "&pid=" + portalId + "&hsutk=" + encodeURIComponent(this.__utk) + "&canon=" + encodeURIComponent(this.canonicalURL) + "&__hssc=" + this.__hssc + "&__hstc=" + this.__hstc + "&email=" + encodeURIComponent(this.email);
        hsjs.src = hsjs.src + "&utm_referrer=" + encodeURIComponent(document.referrer);
        if(this.contentPageId) {
            hsjs.src = hsjs.src + "&contentPageId=" + this.contentPageId;
        }

        (document.getElementsByTagName("head")[0]||document.getElementsByTagName("body")[0]).appendChild(hsjs);

        setTimeout(function() {
            try {
                self.log(placement + " trying to unhide default cta");
                document.getElementById("hs-cta-" + placement).style.visibility="visible";
            } catch(err) {
                self.log(placement + " was already gone");
            }
        }, 2500);
    },

    getCookieValue: function(name) {
        var regex=new RegExp('(^|; )' + name + '=([^;]*)');
        var match = regex.exec(document.cookie);
        return match ? match[2] : "";
    },

    setContentPageId: function(contentMetadata) {
        if(contentMetadata && contentMetadata.contentPageId) {
            this.contentPageId = contentMetadata.contentPageId;
        }
    }

};
