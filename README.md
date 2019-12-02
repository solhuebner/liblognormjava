How to build liblognormjava
---------------------------

This document assumes CentOS 7 and your commands might differ slightly!

You can run the example via:
```
javac test.java && java test
```

It will also create a debug.log (and if there are errors a error.log file).

And you should get the following output:
```
Loaded liblognorm library
Loaded sample rulebase
JsonET: { "r4": "fourth field", "r3": "third field", "r2": "second field", "r1": "first field", "event.tags": [ "csv" ] }
Json  : { "r4": "fourth field", "r3": "third field", "r2": "second field", "r1": "first field" }
XML   : <event><event.tags><tag>csv</tag></event.tags><field name="r4"><value>fourth field</value></field><field name="r3"><value>third field</value></field><field name="r2"><value>second field</value></field><field name="r1"><value>first field</value></field></event>
CEE   : [cee@115 event.tags="csv" r4="fourth field" r3="third field" r2="second field" r1="first field"]
```

Rsyslog
-------

Use a more current version of the rsyslog libs
```
cd /etc/yum.repos.d/
wget http://rpms.adiscon.com/v8-stable/rsyslog.repo
```

SWIG
----

Install swig 4.0.1 from source http://www.swig.org/ (the yum file is pretty old!):
```
yum groupinstall 'Development Tools'
yum remove swig
yum remove rsyslog
yum install pcre-devel
yum install prelink
yum install boost-devel
cd ~
wget http://prdownloads.sourceforge.net/swig/swig-4.0.1.tar.gz
tar xvzf swig-4.0.1.tar.gz
cd swig-4.0.1
./configure
make
make check
make install
```

liblognorm
----------

liblognorm does need libestr-devel but that could be also provided by yum
```
yum install libestr-devel rsyslog
```

```
wget http://www.liblognorm.com/files/download/liblognorm-2.0.6.tar.gz
tar xvzf liblognorm-2.0.6.tar.gz
cd liblognorm-2.0.6

./configure CFLAGS="-g -O2 -fPIC"
```

**-fPIC** is the important part:
> Position Independent Code means that the generated machine code is not dependent on being located at a specific address in order to work.

Modify liblognorm.c in src
--------------------------
Add the following to liblognorm.c:

```
#include "enc.h"
char *
ln_normalize_sh(ln_ctx ctx, const char *const rawmsg, const char *encoding, const int eventTags)
{
	// encoding [json, xml, cee-syslog, //csv]
	// eventTags [0, 1]
	
	char * return_string;
	int length = strlen(rawmsg);
	int convert = 0;
	
	if (length>0) {

		ln_ctx parseCtx = ln_initCtx();
		
		parseCtx->opts = ctx->opts;
		parseCtx->pdag = ctx->pdag;
		parseCtx->pas = ctx->pas;
		
		struct json_object * json = NULL;
		ln_normalize(parseCtx, rawmsg, strlen(rawmsg), &json);

		parseCtx->pdag = NULL;
		parseCtx->pas = NULL;

		if (parseCtx) {
			ln_exitCtx(parseCtx);
		}
		
		if (json != NULL) {
			
			if (strcmp(encoding, "json") != 0) {
				es_str_t *str = NULL;
				
				if (strcmp(encoding, "cee-syslog") == 0) {
					ln_fmtEventToRFC5424(json, &str);
					convert = 1;
				} else if (strcmp(encoding, "xml") == 0) {
					ln_fmtEventToXML(json, &str);
					convert = 1;
				//} else if (strcmp(encoding, "csv") == 0) {
				//	ln_fmtEventToCSV(json, &str, encFmt);
				//	convert = 1;
				} else {
					asprintf(&return_string, "liblognormjava_ERROR: unsupported encoding format chosen: %s", encoding);
				}
				
				if (convert) {
					if (str != NULL) {
						return_string = es_str2cstr(str, NULL);
						es_deleteStr(str);			
					} else {
						asprintf(&return_string, "liblognormjava_ERROR: issue converting to '%s' of the rawmsg '%s'", encoding, rawmsg);
					}
				}
				
			} else {
				if (!eventTags) {
					json_object_object_del(json, "event.tags");
				}
				asprintf(&return_string, "%s", json_object_to_json_string(json));
			}
			json_object_put(json);
			json = NULL;
			
		} else {
			asprintf(&return_string, "liblognormjava_ERROR: ln_normalize returned NULL for rawmsg '%s'", rawmsg);
		}
		
	} else {
		asprintf(&return_string, "liblognormjava_ERROR: rawmsg was too short '%s'", rawmsg);
	}
	
	return return_string;
}

char *
genDOT_string_sh(ln_ctx ctx)
{
	char * return_string;
	es_str_t *str = NULL;
	
	str = es_newStr(1024);
	
	ln_genDotPDAGGraph(ctx->pdag, &str);
	
	if (str != NULL) {
		return_string = es_str2cstr(str, NULL);
		es_deleteStr(str);
	} else {
		asprintf(&return_string, "%s", "liblognormjava_ERROR: ln_genDotPDAGGraph did not return a result");
	}
	
	return return_string;
}

int
genDOT_file_sh(ln_ctx ctx, const char * dotFile)
{
	FILE * fpDOT;
	es_str_t *str = NULL;
	int ret = 0;
	
	if((fpDOT = fopen(dotFile, "w")) == NULL) {
		ret = 1;
	} else {

		str = es_newStr(1024);
		
		ln_genDotPDAGGraph(ctx->pdag, &str);
		
		if (str != NULL) {

			fwrite(es_getBufAddr(str), 1, es_strlen(str), fpDOT);
			
			ret = 0;
			es_deleteStr(str);
			
		} else {
			ret = 1;
		}
		
		fclose(fpDOT);

	}
	
	return ret;
}

static char * logFile_err_sh;

static void
errCallBack_sh(void __attribute__((unused)) *cookie, const char *msg,
	    size_t __attribute__((unused)) lenMsg)
{
	if (msg != NULL) {

		FILE * fpLog;
	
		if((fpLog = fopen(logFile_err_sh, "a")) == NULL) {
			return;
		} else {
			
			char * logMessage;
			
			asprintf(&logMessage, "liblognormjava_ERROR: %s\n", msg);
			
			fputs(logMessage, fpLog);

			free(logMessage);

			fclose(fpLog);
		}
		
	}
}

static char * logFile_dbg_sh;

static void
dbgCallBack_sh(void __attribute__((unused)) *cookie, const char *msg,
	    size_t __attribute__((unused)) lenMsg)
{
	if (msg != NULL) {

		FILE * fpLog;
	
		if((fpLog = fopen(logFile_dbg_sh, "a")) == NULL) {
			return;
		} else {
			
			char * logMessage;
			
			asprintf(&logMessage, "liblognormjava_ERROR: %s\n", msg);
			
			fputs(logMessage, fpLog);

			free(logMessage);

			fclose(fpLog);
		}
		
	}
}

int ln_setErrMsgCB_sh(ln_ctx ctx, const char * logFilename) {
	
	asprintf(&logFile_err_sh, "%s", logFilename);

	return ln_setErrMsgCB(ctx, errCallBack_sh, NULL);
}

int ln_setDbgMsgCB_sh(ln_ctx ctx, const char * logFilename) {
	
	asprintf(&logFile_dbg_sh, "%s", logFilename);

	ln_enableDebug(ctx, 1);

	return ln_setDebugCB(ctx, dbgCallBack_sh, NULL);
}
```

Modify liblognorm.h in src
--------------------------
Add the following to the end of liblognorm.h before #endif /* #ifndef LOGNORM_H_INCLUDED */

```
char * ln_normalize_sh(ln_ctx ctx, const char *const rawmsg, const char *encoding, const int eventTags);
char * genDOT_string_sh(ln_ctx ctx);
int genDOT_file_sh(ln_ctx ctx, const char * dotFile);
int ln_setErrMsgCB_sh(ln_ctx ctx, const char * logFilename);
int ln_setDbgMsgCB_sh(ln_ctx ctx, const char * logFilename);
```

Build the modified liblognorm
-----------------------------
```
make
```

Use Swig to create wrapper
--------------------------
```
cd src
```

Create **liblognormjava.i** with the following content:
```
%module liblognorm
%{
/* Includes the header in the wrapper code */
#include "liblognorm.h"
#include "parser.h"
#include "pdag.h"
#include "samp.h"
#include "lognorm.h"
#include "internal.h"
#include "helpers.h"
#include "annot.h"
#include "enc.h"
%}

/* Parse the header file to generate wrappers */
%include "liblognorm.h"
%include "parser.h"
%include "annot.h"
%include "enc.h"
```

```
swig -java -package sh.tools.liblognormjava liblognormjava.i
gcc -fPIC -c liblognormjava_wrap.c -I/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.232.b09-0.el7_7.x86_64/include -I/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.232.b09-0.el7_7.x86_64/include/linux -I/usr/include/libfastjson
ld -G liblognormjava_wrap.o .libs/liblognorm.a /usr/lib64/libfastjson.so.4.2.0 /usr/lib64/libestr.so.0.0.0 -o /tmp/liblognormjava.so
```

Notes
-----

**-fPIC** is the important part:
> Position Independent Code means that the generated machine code is not dependent on being located at a specific address in order to work.

Building against the static version of liblognorm (**liblognorm.a**) makes it portable.

**-I/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.232.b09-0.el7_7.x86_64/include** might be different on your system as it is depending on the installed Java version.

**/tmp/liblognormjava.so** is the new shared library that we can load from Java.

The **-package** part will be reflected in the generated java files. These need to be copied to the project that will use liblognormjava.
