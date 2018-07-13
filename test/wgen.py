

'''
This script can be used to generate large numbers of warnings with little effort.
It generates a set of warnings for each line in each compilation unit, where
the number per line is controlled by the variable WARNINGS_PER_LINE below.

If supplied by Grammatech for Praqma for testing the Code Sonar plugin.


There are three approaches to using it; 1) plugin approach for all analysis running 2) plugin approach for single project 3) interactive runninng command from command line


1) Use at Code Sonar plugin for all analysis running, but adding the following line to the standard template configuration file usually found in <codesonar installation path>/template.conf

    PLUGINS += wgen.py

2) Add the plugin line to a project configuration file. Copy the default template from <codesonar installation path>/template.conf to project workspace as wgen-codesonar-template.conf, and run command:

    codesonar analyze <PROJNAME< -foreground -conf-file wgen-codesonar-template.conf <HUB IP:PORT> <project compile, eg. make -j2>

3) Use interactively (useful for debugging) do something like this:
  codesonar analyze -foreground -preset python_debug_console Wgen riddle:9450 gcc -c wgen.c
At the prompt do:
  execfile("wgen.py")
  go()
Quit the interpreter to let the analysis finish and so that the warnings show up in the hub.
'''

import cs

@cs.compunit_visitor
def do_cu(cu):
    if not cu.is_user():
        return
    WARNINGS_PER_LINE = 2
    wcs = []
    for i in range(WARNINGS_PER_LINE):
        wcs.append(cs.analysis.create_warningclass("wgen.py plugin generated warning %d" % i))
    sfi = cu.get_sfileinst()
    for i in range(1,sfi.line_count()):
        for wc in wcs:
            wc.report(sfi, i, "This is line %d from wgen.py plugin" % i)

def go():
    for cu in cs.project.current().compunits():
        do_cu(cu)
