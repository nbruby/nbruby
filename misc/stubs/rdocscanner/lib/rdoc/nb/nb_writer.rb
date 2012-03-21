require 'fileutils'

module NB
  class NbWriter

    def NbWriter.class_desc_path(dir, class_desc)
      # BEGIN NETBEANS MODIFICATIONS
      #File.join(dir, "cdesc-" + class_desc.name + ".yaml")
      # TODO - I need to preserve filenames and paths for files
      # that should correspond to requires - this probably
      # is stored in nb_generator as part of the file include list      
       File.join(dir, "cdesc-" + class_desc.name + ".rb")
      # END NETBEANS MODIFICATIONS
    end

    
    # Convert a name from internal form (containing punctuation)
    # to an external form (where punctuation is replaced
    # by %xx)

    def NbWriter.internal_to_external(name)
      name.gsub(/\W/) { sprintf("%%%02x", $&[0]) }
    end

    # And the reverse operation
    def NbWriter.external_to_internal(name)
      name.gsub(/%([0-9a-f]{2,2})/) { $1.to_i(16).chr }
    end

    def initialize(base_dir)
      @base_dir = base_dir
    end

    def remove_class(class_desc)
      FileUtils.rm_rf(path_to_dir(class_desc))
    end

# Original RI implementation
#    def add_class(class_desc)
#      dir = path_to_dir(class_desc.full_name)
#      FileUtils.mkdir_p(dir)
#      class_file_name = NbWriter.class_desc_path(dir, class_desc)
#      File.open(class_file_name, "w") do |f|
#        f.write(class_desc.serialize)
#      end
#    end
#
#    def add_method(class_desc, method_desc)
#      dir = path_to_dir(class_desc.full_name)
#      file_name = NbWriter.internal_to_external(method_desc.name)
#      meth_file_name = File.join(dir, file_name)
#      if method_desc.is_singleton
#        meth_file_name += "-c.yaml"
#      else
#        meth_file_name += "-i.yaml"
#      end
#
#      File.open(meth_file_name, "w") do |f|
#        f.write(method_desc.serialize)
#      end
#    end
#
    
    def add_class(class_desc)
      dir = path_to_dir(class_desc)
      FileUtils.mkdir_p(dir)
      class_file_name = NbWriter.class_desc_path(dir, class_desc)
#      puts "Writing class " + class_file_name
#      class_file_name = NbWriter.class_desc_path(@base_dir, class_desc)
      File.open(class_file_name, "w") do |f|
#        f.write(class_desc.serialize)
        f.write(class_desc.stubify)
#      puts "  done with " + class_file_name
      end
    end

    def add_method(class_desc, method_desc)
      dir = path_to_dir(class_desc)
      file_name = NbWriter.internal_to_external(method_desc.name)
#      puts "Writing method " + file_name
      meth_file_name = File.join(dir, file_name)
      if method_desc.is_singleton
        meth_file_name += "-c.rb"
      else
        meth_file_name += "-i.rb"
      end

      File.open(meth_file_name, "w") do |f|
#        f.write(method_desc.serialize)
        f.write(method_desc.stubify)
      end
#      puts "  done"
    end

    
    
    
    private

    def path_to_dir(class_desc)
      class_name = class_desc.full_name
      filename = class_desc.filename
      dir = @base_dir
      if (filename != nil)
        dir = File.join(dir, filename)
      end
      FileUtils.mkdir_p(dir)
      File.join(dir, *class_name.split('::'))
    end
  end
end
