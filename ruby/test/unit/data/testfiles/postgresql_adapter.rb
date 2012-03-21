# Boiled down code snippet from postgresql_adapter.rb which trips up the
# lexer; the query call is not properly lexed
module ActiveRecord
  def indexes(table_name, name = nil) #:nodoc:
    result = query(<<-SQL, name)
      SELECT i.relname, d.indisunique, a.attname
        FROM pg_class t, pg_class i, pg_index d, pg_attribute a
       WHERE i.relkind = 'i'
         AND d.indexrelid = i.oid
         AND d.indisprimary = 'f'
         AND t.oid = d.indrelid
         AND t.relname = '#{table_name}'
         AND a.attrelid = t.oid
         AND ( d.indkey[0]=a.attnum OR d.indkey[1]=a.attnum
            OR d.indkey[2]=a.attnum OR d.indkey[3]=a.attnum
            OR d.indkey[4]=a.attnum OR d.indkey[5]=a.attnum
            OR d.indkey[6]=a.attnum OR d.indkey[7]=a.attnum
            OR d.indkey[8]=a.attnum OR d.indkey[9]=a.attnum )
      ORDER BY i.relname
    SQL
  end

  def columns(table_name, name = nil) #:nodoc:
    column_definitions(table_name).collect do |name, type, default, notnull, typmod|
      # typmod now unused as limit, precision, scale all handled by superclass
      Column.new(name, default_value(default), translate_field_type(type), notnull == "f")
    end
  end
end
