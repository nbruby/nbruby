def foo(unusedparam, unusedparam2, usedparam)
  unusedparam2 = 5 # Written but not read - still unused!
  unusedlocal1 = "foo"
  usedlocal2 = "hello"
  usedlocal3 = "world"
  puts usedparam
  x = []
  x.each { |unusedblockvar1, usedblockvar2|
    puts usedblockvar2
    puts usedlocal2
  } 
  puts usedlocal3
end

