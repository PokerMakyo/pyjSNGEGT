import jpype

jpype.startJVM(jpype.getDefaultJVMPath(), ('-ea', '-Djava.classpath.path=.'))

_cr = jpype.JPackage("pokerai").game.icm.CalcRanges()

def _list_to_jarray(jtype, plist):
    jarray = jpype.JArray(jtype)(len(plist))

    for i, val in enumerate(plist):
        jarray[i] = jtype(val)

    return jarray 

def calc(players, stacks, raiser, bb, ante, nosb, evthreshold, payouts):
    jplayers = jpype.JInt(players)
    jstacks = _list_to_jarray(jpype.JInt, stacks)
    jranges = jpype.JArray(jpype.JInt)(players)
    jraisor = jpype.JInt(raisor)
    jBB = jpype.JInt(bb)
    jante = jpype.JInt(ante)
    jnoSmallBlind = jpype.JBoolean(noSmallBlind)
    jevTreshold = jpype.JDouble(evTreshold)
    jpayouts = _list_to_jarray(jpype.JDouble, payouts)

    _cr.calc(jplayers, jstacks, jraisor, jBB, jante, jnoSmallBlind, jevTreshold, jpayouts, jranges);
    return [r for r in jranges]

if __name__ == "__main__":
    # Example 1
    players = 7;
    payouts = [0.50, 0.30, 0.20, 0, 0, 0, 0]
    stacks = [5000, 5000, 5000, 5000, 5000, 5000, 5000]
    raisor = 2  # my position when there is no raised before me
    BB = 300;
    ante = 0;
    noSmallBlind = False;
    evTreshold = 0;
    ranges = calc(players, stacks, raisor, BB, ante, noSmallBlind, evTreshold, payouts);

    print "Push Example (#1):"
    for i, r in enumerate(ranges):
        print "#%i: %i%%" % (i, r)
    print  

    # Example 2
    players = 7
    payouts = [0.50, 0.30, 0.20, 0, 0, 0, 0]
    stacks = [5000, 5000, 5000, 5000, 5000, 5000, 5000]
    raisor = 5  # my position when there is no raised before me
    BB = 300
    ante = 0
    noSmallBlind = False
    evTreshold = 0
    ranges = calc(players, stacks, raisor, BB, ante, noSmallBlind, evTreshold, payouts)

    print "Call Example (#2):"
    for i, r in enumerate(ranges):
        print "#%i: %i%%" % (i, r)
    print  

# vim: filetype=python syntax=python expandtab shiftwidth=4 softtabstop=4
