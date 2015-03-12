My java homework:

1) HW1 - easy (use try resources, print readable messages, check args, args[0], args[1] for null)

2) HW2 - use Collections.binarySearch(), extend AbstractSet (removes some unnecessary code), don't use not standard annotations (for example javax.annotation.Nonnull).
Note, that descendingSet() works for O(1) and headSet(), tailSet(), subSet() work for O(log n).
You can think about using Comparator.naturalOrder()

3) HW3 - imports are unneseccary (because there is no easy way to resolve equal name conflicts), jedi version (generics) give you 5 bonuses,
also you can earn 1 bonus by getting the shortest code.
I've earned 5 bonuses by showing my own generic tests (they convinced him somehow)

4) HW4 - it's easy. Ensure, that your program works on Windows too (i've got one minus for this). 
build.sh compiles a .jar file

5) HW5 - it's also easy. GK doesn't look at javadoc very thoroughly, but all your link must be clickable
docgenerator.sh generates javadoc for implementor