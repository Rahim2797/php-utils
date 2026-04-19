<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Product extends Model
{
    protected $fillable = [
        'sku',
        'title',
        'price_cents',
        'is_active',
        'published_at',
    ];

    public string $transientLabel = '';

    protected function casts(): array
    {
        return [
            'is_active' => 'bool',
            'price_cents' => 'int',
            'published_at' => 'datetime',
        ];
    }
}
