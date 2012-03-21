require 'rbconfig'

# Used by NetBeans to gather information about a Ruby Platform. Output is
# properties file format so NetBeans can easily parse it through
# java.util.Properties

# See issue #154660
def escape(path)
  path.gsub('%', '%%')
end

# Interpreter info
jruby = defined?(JRUBY_VERSION) || (defined?(RUBY_ENGINE) && 'jruby' == RUBY_ENGINE)
rubinius = defined?(RUBY_ENGINE) && 'rbx' == RUBY_ENGINE
$stdout.printf "ruby_kind=#{jruby ? "JRuby" : (rubinius ? "Rubinius" : "Ruby")}\n"
$stdout.printf "ruby_version=#{RUBY_VERSION}\n"
$stdout.printf "jruby_version=#{JRUBY_VERSION}\n" if jruby
$stdout.printf "ruby_patchlevel=#{RUBY_PATCHLEVEL}\n" if defined? RUBY_PATCHLEVEL
$stdout.printf "ruby_release_date=#{RUBY_RELEASE_DATE}\n"
RbConfig = Config unless defined?(RbConfig) # 1.8.4 support
ruby = File.join(RbConfig::CONFIG["bindir"], RbConfig::CONFIG["ruby_install_name"])
ruby << RbConfig::CONFIG["EXEEXT"]
$stdout.printf "ruby_executable=#{escape(ruby)}\n"
$stdout.printf "ruby_platform=#{RUBY_PLATFORM}\n"
$stdout.printf "ruby_lib_dir=#{escape(RbConfig::CONFIG['rubylibdir'])}\n"

# RubyGems info
begin
  require 'rubygems'
rescue LoadError
  # no RubyGems installed
  $stdout.printf "\n"
  exit 0
end
$stdout.printf "gem_home=#{escape(Gem.dir)}\n"
$stdout.printf "gem_path=#{Gem.path.map{|p| escape(p)}.join(":")}\n"
$stdout.printf "gem_version=#{Gem::RubyGemsVersion}\n"
$stdout.printf "\n"
