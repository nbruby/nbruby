# The following if/unless statements should be convertable
x = 5
if !true
  puts "hello"
end

if x != 5
  puts "hello"
end


if (x != 6)
  puts "world"
end

unless !x
  puts "test"
end

if !(x < 12) then puts "hello"; end

unless !(x < 5)
  puts "Hello"
end

if !(x < 8)
  puts "Hello"
  puts "World"
end

if (x != 9) then puts "hello"; puts "world"; end

x = 10 if x != 11
x = 10 if (x != 14)

unless (x != 6)
  puts "Hello"
  puts "World"
end

x = 11 unless x != 12
x = 11 unless (x != 13)


